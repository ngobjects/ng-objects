package ng.appserver.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGApplication;
import ng.appserver.NGContext;

public class NGStandardRequest implements NGRequest {

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
	 * The request's form values (a.k.a. query parameters)
	 */
	private final Map<String, List<String>> _formValues;

	/**
	 * Values of cookies
	 */
	private final Map<String, List<String>> _cookieValues;

	/**
	 * The network address of the client, as reported by the adaptor (see NGRequest.remoteAddress()).
	 * Set post-construction by the adaptor rather than through the (already wide) constructor.
	 */
	private final String _remoteAddress;

	/**
	 * FIXME: The httpVersion parameter is currently not used. I'm keeping it around to keep the constructor's shape, since I _think_ we might eventually want to use it. But today, It's just added weight 	// Hugi 2026-05-12
	 */
	public NGStandardRequest( final String method, final String uri, final String httpVersion, final Map<String, List<String>> headers, final Map<String, List<String>> formValues, final Map<String, List<String>> cookieValues, final InputStream contentStream, String remoteAddress ) {
		Objects.requireNonNull( method );
		Objects.requireNonNull( uri );
		Objects.requireNonNull( httpVersion );
		Objects.requireNonNull( headers );
		Objects.requireNonNull( contentStream );

		_method = method;
		setURI( uri );
		//		setHttpVersion( httpVersion );
		setHeaders( headers );
		_formValues = formValues; // FIXME: Should be populated by the request object, not the adaptor // Hugi 2021-12-31
		_cookieValues = cookieValues; // FIXME: Cookie header deserialization should happen in NGRequest instead of in the adaptor // Hugi 2021-12-30
		_remoteAddress = remoteAddress;
		_contentStream = contentStream;
	}

	@Override
	public String remoteAddress() {
		return _remoteAddress;
	}

	/**
	 * @return The requests form values (query parameters)
	 */
	@Override
	public Map<String, List<String>> formValues() {
		return _formValues;
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

	@Override
	public String formValueForKey( final String key ) {
		final List<String> values = formValuesForKey( key );

		if( values.isEmpty() ) {
			return null;
		}

		// Fail if multiple form values are present for the same query parameter.
		if( values.size() > 1 ) {
			throw new IllegalStateException( "The request contains %s form values named '%s' with values (%s). If you expected multiple values, use formValuesForKey() instead of formValueForKey()".formatted( values.size(), key, values ) );
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
	public Map<String, List<String>> cookieValues() {
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
	 * @return The value of the named cookie if there's only one cookie with that name.
	 * @throws IllegalArgumentException If there are many cookies with the given key
	 */
	@Override
	public String cookieValueForKey( final String key ) {
		Objects.requireNonNull( key );

		final List<String> values = cookieValuesForKey( key );

		if( values.size() == 0 ) {
			return null;
		}

		if( values.size() > 1 ) {
			throw new IllegalArgumentException( "There's more than one cookie with the name '%s'".formatted( key ) );
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
	 * FIXME: This is temporary handling of multipart file uploads. Still thinking on the final design of this // Hugi 2025-04-05
	 */
	private final Map<String, UploadedFile> _uploadedFiles = new HashMap<>();

	public record UploadedFile( String name, String contentType, InputStream stream, long length ) {}

	@Override
	public Map<String, UploadedFile> _uploadedFiles() {
		return _uploadedFiles;
	}

	@Override
	public String toString() {
		return "NGRequest [_method=" + _method + ", _uri=" + _uri + ", _headers=" + headers() + ", _formValues=" + _formValues + ", _cookieValues=" + _cookieValues + "]";
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
		if( _contentByteStream == null ) {
			final ByteArrayOutputStream contentByteStream = new ByteArrayOutputStream();

			try( _contentStream ) {
				_contentStream.transferTo( contentByteStream );
			}
			catch( IOException e ) {
				throw new UncheckedIOException( e );
			}

			_contentByteStream = contentByteStream;
		}

		return _contentByteStream;
	}
}