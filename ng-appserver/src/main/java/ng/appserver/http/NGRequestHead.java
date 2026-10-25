package ng.appserver.http;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGApplication;
import ng.appserver.NGContext;

/**
 * What a request is apart from its body: its request line, headers and origin, and what's parsed from them.
 * The request implementations, which differ in their body, each hold one.
 */

final class NGRequestHead {

	private final String _method;

	/**
	 * FIXME: This should really not be settable, NGRequest wants to be immutable // Hugi 2024-08-10
	 */
	private String _uri;

	/**
	 * The URI's query string, still percent-encoded. null if the URI has none.
	 */
	private final String _queryString;

	/**
	 * The request's headers. Names are case-insensitive.
	 */
	private final Map<String, List<String>> _headers = NGHeaderMaps.create();

	/**
	 * The network address of the client, as reported by the adaptor. null if the adaptor didn't provide one.
	 */
	private final String _remoteAddress;

	private NGContext _context;

	/**
	 * FIXME: The httpVersion parameter is currently not used. I'm keeping it around to keep the constructor's shape, since I _think_ we might eventually want to use it. But today, It's just added weight 	// Hugi 2026-05-12
	 */
	NGRequestHead( final String method, final String uri, final String queryString, final String httpVersion, final Map<String, List<String>> headers, final String remoteAddress ) {
		Objects.requireNonNull( method );
		Objects.requireNonNull( uri );
		Objects.requireNonNull( httpVersion );
		Objects.requireNonNull( headers );

		_method = method;
		_uri = uri;
		_queryString = queryString;

		setHeaders( headers );
		_remoteAddress = remoteAddress;
	}

	String method() {
		return _method;
	}

	String uri() {
		return _uri;
	}

	void setURI( final String uri ) {
		_uri = uri;
	}

	String queryString() {
		return _queryString;
	}

	/**
	 * @return The request's headers. Names are case-insensitive, and neither the map nor its value lists can be modified.
	 */
	Map<String, List<String>> headers() {
		return Collections.unmodifiableMap( _headers );
	}

	/**
	 * Replaces the request's headers with the given ones
	 */
	void setHeaders( final Map<String, List<String>> headers ) {
		_headers.clear();

		for( final Map.Entry<String, List<String>> header : headers.entrySet() ) {
			_headers.put( header.getKey(), List.copyOf( header.getValue() ) );
		}
	}

	String remoteAddress() {
		return _remoteAddress;
	}

	/**
	 * @return The request's content type, from its Content-Type header. null if it has none.
	 */
	String contentType() {
		final List<String> values = _headers.get( "content-type" );
		return values == null || values.isEmpty() ? null : values.get( 0 );
	}

	/**
	 * @return The query string's form values, freshly parsed. Empty if the URI has no query string.
	 * @throws IllegalArgumentException If the query string has more than maxFields fields
	 */
	Map<String, List<String>> parseQueryValues( final int maxFields ) {
		if( _queryString == null ) {
			return new LinkedHashMap<>();
		}

		return NGFormValues.parse( _queryString.getBytes( StandardCharsets.UTF_8 ), StandardCharsets.UTF_8, maxFields );
	}

	/**
	 * @return The values of the request's cookies, parsed from its Cookie headers. Parsed on each invocation, since the headers can change.
	 */
	Map<String, List<String>> cookieValues() {
		final List<String> cookieHeaders = _headers.get( "cookie" );
		return NGCookieValues.parse( cookieHeaders == null ? List.of() : cookieHeaders );
	}

	/**
	 * FIXME: This method needs to be thread safe // Hugi 2021-11-28
	 *
	 * @param request The request this is the head of
	 */
	NGContext context( final NGRequest request ) {
		if( _context == null ) {
			_context = NGApplication.application().createContextForRequest( request );
		}

		return _context;
	}

	void setContext( final NGContext context ) {
		_context = context;
	}

	/**
	 * @return A description of the request, for its toString()
	 */
	String describe() {
		return "_method=" + _method + ", _uri=" + _uri + ", _queryString=" + _queryString + ", _headers=" + _headers;
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
}
