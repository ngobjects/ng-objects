package ng.appserver.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGApplication;
import ng.appserver.NGContext;

public class NGStandardRequest implements NGRequest {

	/**
	 * The most fields read from the query string, and from a form-encoded body
	 */
	public static final int MAX_FORM_FIELDS = 1000;

	/**
	 * The largest form-encoded body read for form values, in bytes
	 */
	public static final int MAX_FORM_CONTENT_LENGTH = 200_000;

	/**
	 * The request's context
	 */
	private NGContext _context;

	/**
	 * The requests's method
	 */
	private final String _method;

	/**
	 * The URI being accessed
	 */
	private String _uri;

	/**
	 * The URI's query string, still percent-encoded. null if the URI has none.
	 */
	private final String _queryString;

	/**
	 * The request's form values, from the query string and a form-encoded body. null until something asks for them.
	 */
	private Map<String, List<String>> _formValues;

	/**
	 * The values of the request's cookies, parsed from its Cookie headers. null until something asks for them.
	 */
	private Map<String, List<String>> _cookieValues;

	/**
	 * The network address of the client, as reported by the adaptor (see NGRequest.remoteAddress()).
	 * Set post-construction by the adaptor rather than through the (already wide) constructor.
	 */
	private final String _remoteAddress;

	/**
	 * Creates a request whose form values are parsed from the query string and, when the body is form-encoded, the body, the first time they're asked for.
	 *
	 * FIXME: The httpVersion parameter is currently not used. I'm keeping it around to keep the constructor's shape, since I _think_ we might eventually want to use it. But today, It's just added weight 	// Hugi 2026-05-12
	 */
	public NGStandardRequest( final String method, final String uri, final String queryString, final String httpVersion, final Map<String, List<String>> headers, final InputStream contentStream, String remoteAddress ) {
		Objects.requireNonNull( method );
		Objects.requireNonNull( uri );
		Objects.requireNonNull( httpVersion );
		Objects.requireNonNull( headers );
		Objects.requireNonNull( contentStream );

		_method = method;
		setURI( uri );
		//		setHttpVersion( httpVersion );
		setHeaders( headers );
		_queryString = queryString;
		_remoteAddress = remoteAddress;
		_contentStream = contentStream;
	}

	@Override
	public String remoteAddress() {
		return _remoteAddress;
	}

	/**
	 * @return The request's form values: the query string's, followed by the body's if it's form-encoded (application/x-www-form-urlencoded). Parsed on first invocation.
	 * @throws IllegalArgumentException If the query string or the body has more than MAX_FORM_FIELDS fields, or the body is longer than MAX_FORM_CONTENT_LENGTH
	 */
	@Override
	public synchronized Map<String, List<String>> formValues() {
		if( _formValues == null ) {
			_formValues = parseFormValues();
		}

		return _formValues;
	}

	/**
	 * @return The request's form values, parsed. Invoked once, by formValues().
	 */
	protected Map<String, List<String>> parseFormValues() {
		final Map<String, List<String>> formValues = new LinkedHashMap<>();

		if( _queryString != null ) {
			formValues.putAll( NGFormValues.parse( _queryString.getBytes( StandardCharsets.UTF_8 ), StandardCharsets.UTF_8, MAX_FORM_FIELDS ) );
		}

		final String contentType = headerForKey( "content-type" );

		if( contentType != null && "application/x-www-form-urlencoded".equalsIgnoreCase( mediaType( contentType ) ) ) {
			final Map<String, List<String>> bodyValues = NGFormValues.parse( readContent( MAX_FORM_CONTENT_LENGTH ).toByteArray(), charset( contentType ), MAX_FORM_FIELDS );

			for( final Entry<String, List<String>> entry : bodyValues.entrySet() ) {
				formValues.computeIfAbsent( entry.getKey(), _unused -> new ArrayList<>() ).addAll( entry.getValue() );
			}
		}

		return formValues;
	}

	/**
	 * @return The given content type without its parameters
	 */
	static String mediaType( final String contentType ) {
		final int semicolon = contentType.indexOf( ';' );
		return (semicolon == -1 ? contentType : contentType.substring( 0, semicolon )).trim();
	}

	/**
	 * @return The value of the given content type's named parameter, without quotes. null if it doesn't have it.
	 */
	static String parameter( final String contentType, final String name ) {
		final String[] parts = contentType.split( ";" );

		for( int i = 1; i < parts.length; i++ ) {
			final int equals = parts[i].indexOf( '=' );

			if( equals != -1 && name.equalsIgnoreCase( parts[i].substring( 0, equals ).trim() ) ) {
				return parts[i].substring( equals + 1 ).trim().replace( "\"", "" );
			}
		}

		return null;
	}

	/**
	 * @return The charset named by the given content type's charset parameter, UTF-8 if it names none (or one we don't know)
	 */
	static Charset charset( final String contentType ) {
		final String name = parameter( contentType, "charset" );

		if( name != null ) {
			try {
				return Charset.forName( name );
			}
			catch( IllegalCharsetNameException | UnsupportedCharsetException e ) {
				return StandardCharsets.UTF_8;
			}
		}

		return StandardCharsets.UTF_8;
	}

	/**
	 * @return The form values represented by given key/query parameter. Returns an empty list if the form value is not present
	 */
	@Override
	public List<String> formValuesForKey( final String key ) {
		final List<String> list = formValues().get( key );

		if( list == null ) {
			return Collections.emptyList();
		}

		return list;
	}

	/**
	 * @return The first form value with the given name. null if there's none.
	 */
	@Override
	public String formValueForKey( final String key ) {
		final List<String> values = formValuesForKey( key );

		if( values.isEmpty() ) {
			return null;
		}

		return values.get( 0 );
	}

	/**
	 * @return the request's URL
	 *
	 * CHECKME: We might want to consider renaming this "url". Conventions are different between frameworks though, for example, java.net.HttpRequest uses "uri" // Hugi 2024-06-29
	 */
	@Override
	public String uri() {
		return _uri;
	}

	/**
	 * FIXME: This should really not be settable, NGRequest wants to be immutable // Hugi 2024-08-10
	 */
	@Override
	public void setURI( final String uri ) {
		_uri = uri;
	}

	/**
	 * @return The request's method
	 */
	@Override
	public String method() {
		return _method;
	}

	/**
	 * @return The sessionID submitted by the client, if any. null if no sessionID was present in the request.
	 */
	@Override
	public String _sessionID() {
		return cookieValueForKey( NGApplication.SESSION_ID_COOKIE_NAME );
	}

	/**
	 * @return A map of cookie values from the request.
	 */
	@Override
	public synchronized Map<String, List<String>> cookieValues() {
		if( _cookieValues == null ) {
			_cookieValues = NGCookieValues.parse( headersForKey( "cookie" ) );
		}

		return _cookieValues;
	}

	/**
	 * @return The values of the named cookie
	 */
	@Override
	public List<String> cookieValuesForKey( final String key ) {
		Objects.requireNonNull( key );

		final List<String> cookieValues = cookieValues().get( key );

		if( cookieValues == null ) {
			return Collections.emptyList();
		}

		return cookieValues;
	}

	/**
	 * @return The value of the first cookie with the given name. null if there's none.
	 */
	@Override
	public String cookieValueForKey( final String key ) {
		Objects.requireNonNull( key );

		final List<String> values = cookieValuesForKey( key );

		if( values.isEmpty() ) {
			return null;
		}

		return values.get( 0 );
	}

	/**
	 * FIXME: This method needs to be thread safe // Hugi 2021-11-28
	 */
	@Override
	public NGContext context() {
		if( _context == null ) {
			_context = NGApplication.application().createContextForRequest( this );
		}

		return _context;
	}

	@Override
	public void _setContext( NGContext context ) {
		_context = context;
	}

	/**
	 * Invoked by the adaptor once the request has been handled and its response sent, to release whatever the request holds on to
	 */
	public void _close() {}

	@Override
	public String toString() {
		return "NGRequest [_method=" + _method + ", _uri=" + _uri + ", _queryString=" + _queryString + ", _headers=" + headers() + "]";
	}

	/* ------ FIXME: below is logic from NGMessage, awaiting to be nicely factored into the class // Hugi 2026-05-12 ------  */

	/**
	 * The headers  of this message
	 */
	private Map<String, List<String>> _headers = NGMessage.createEmptyHeadersMap();

	/**
	 * The request's body as received from the adaptor, read on first access to the content. Readable only while the adaptor is handling the request.
	 */
	private final InputStream _contentStream;

	/**
	 * The request's body, once read. null until something asks for the content.
	 */
	private ByteArrayOutputStream _contentByteStream;

	/**
	 * @return The HTTP headers of this message
	 */
	@Override
	public Map<String, List<String>> headers() {
		return _headers;
	}

	/**
	 * Sets the headers from the given map.
	 */
	@Override
	public void setHeaders( final Map<String, List<String>> newHeaders ) {
		_headers = NGMessage.createEmptyHeadersMap();

		for( Entry<String, List<String>> header : newHeaders.entrySet() ) {
			_headers.put( header.getKey(), header.getValue() );
		}
	}

	/**
	 * @return The request's content, read from the adaptor's stream on first invocation
	 */
	@Override
	public synchronized ByteArrayOutputStream contentByteStream() {
		return readContent( Long.MAX_VALUE );
	}

	/**
	 * @return The request's content, read from the adaptor's stream if it hasn't been yet
	 * @throws IllegalArgumentException If the content has to be read and turns out longer than maxLength bytes
	 */
	private synchronized ByteArrayOutputStream readContent( final long maxLength ) {
		if( _contentByteStream == null ) {
			final ByteArrayOutputStream contentByteStream = new ByteArrayOutputStream();

			try( _contentStream ) {
				final byte[] buffer = new byte[8192];
				long length = 0;
				int read;

				while( (read = _contentStream.read( buffer )) != -1 ) {
					length += read;

					if( length > maxLength ) {
						throw new IllegalArgumentException( "Content is longer than %s bytes".formatted( maxLength ) );
					}

					contentByteStream.write( buffer, 0, read );
				}
			}
			catch( IOException e ) {
				throw new UncheckedIOException( e );
			}

			_contentByteStream = contentByteStream;
		}

		return _contentByteStream;
	}
}