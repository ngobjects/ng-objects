package ng.appserver.http;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGApplication;
import ng.appserver.NGContext;

/**
 * Represents a request entering the system
 *
 * A multipart/form-data request is an NGMultipartRequest, which also has the files uploaded with it. NGRequests.of() creates the request a content type calls for.
 *
 * The body arrives once, and can be read in several ways: in full (contentBytes(), contentString()), as a stream (contentStream()), as form values (formValues()), or, for a multipart request, as uploaded files or incoming parts.
 * - Reading it in full (contentBytes(), contentString(), and formValues() on a form-encoded body) keeps the bytes, so later reads of any kind are served from them.
 * - Collecting a multipart request's parts (formValues(), uploadedFiles()) keeps the parts but not the body as it arrived, so later reads of parts are served from them, while reading the body in full or as a stream is refused.
 * - Reading it as a stream (contentStream(), incomingParts()) keeps nothing, so it can't be read again, and form values then hold only the query string's.
 * A read that can't be served says what read the body before, or why reading it failed. contentType() and contentLength() describe the body without reading it.
 *
 * The request's headers are changed through setHeader(), appendHeader() and setHeaders(); the map headers() returns can't be modified.
 */

public interface NGRequest {

	/**
	 * @return the request's method
	 */
	public String method();

	/**
	 * @return the request's URL
	 *
	 * CHECKME: We might want to consider renaming this "url". Conventions are different between frameworks though, for example, java.net.HttpRequest uses "uri" // Hugi 2024-06-29
	 */
	public String uri();

	/**
	 * @return The request's headers, by name. Names are case-insensitive. The map can't be modified; change headers through setHeader(), appendHeader() and setHeaders().
	 */
	public Map<String, List<String>> headers();

	/**
	 * Replaces the request's headers with the given ones
	 */
	public void setHeaders( final Map<String, List<String>> headers );

	/**
	 * Sets the header with the given name to the given value, replacing any values it had.
	 */
	public default void setHeader( final String headerName, final String value ) {
		Objects.requireNonNull( headerName );
		Objects.requireNonNull( value );

		final Map<String, List<String>> headers = NGHeaderMaps.create();
		headers.putAll( headers() );
		headers.put( headerName, List.of( value ) );
		setHeaders( headers );
	}

	/**
	 * Adds the given value to the header with the given name, after any values it has.
	 */
	public default void appendHeader( final String headerName, final String value ) {
		Objects.requireNonNull( headerName );
		Objects.requireNonNull( value );

		final List<String> values = new ArrayList<>( headersForKey( headerName ) );
		values.add( value );

		final Map<String, List<String>> headers = NGHeaderMaps.create();
		headers.putAll( headers() );
		headers.put( headerName, values );
		setHeaders( headers );
	}

	/**
	 * @return The values of the header with the given name. An empty list if there are none.
	 */
	public default List<String> headersForKey( final String key ) {
		final List<String> values = headers().get( key );

		if( values == null ) {
			return Collections.emptyList();
		}

		return values;
	}

	/**
	 * @return The first value of the header with the given name. null if there's none.
	 */
	public default String headerForKey( final String key ) {
		final List<String> values = headersForKey( key );

		if( values.isEmpty() ) {
			return null;
		}

		return values.get( 0 );
	}

	/**
	 * @return The request's content type, from its Content-Type header. null if it has none. Doesn't read the body.
	 */
	public default String contentType() {
		return headerForKey( "content-type" );
	}

	/**
	 * @return The length of the request's body in bytes, from its Content-Length header. null if it doesn't say (or says something that isn't a length). Doesn't read the body.
	 */
	public default Long contentLength() {
		final String value = headerForKey( "content-length" );

		if( value == null ) {
			return null;
		}

		try {
			return Long.valueOf( value.strip() );
		}
		catch( NumberFormatException e ) {
			return null;
		}
	}

	/**
	 * @return The request's body, read in full and kept. A multipart request's body as it arrived, boundaries and all.
	 * @throws IllegalStateException If the body was already read in a way that didn't keep it
	 */
	public byte[] contentBytes();

	/**
	 * @return The request's body as a stream, for bodies too large to hold in memory. The body is then gone, unless it was already read in full, in which case the stream is over what was kept.
	 * @throws IllegalStateException If the body was already read in a way that didn't keep it
	 */
	public InputStream contentStream();

	/**
	 * @return The request's body, as a string in the charset its content type names (UTF-8 if it names none)
	 */
	public default String contentString() {
		final String contentType = contentType();
		return new String( contentBytes(), contentType == null ? StandardCharsets.UTF_8 : NGRequestHead.charset( contentType ) );
	}

	/**
	 * @return the request's form values
	 */
	public Map<String, List<String>> formValues();

	/**
	 * @return The form values with the given name. An empty list if there are none.
	 */
	public default List<String> formValuesForKey( final String key ) {
		final List<String> values = formValues().get( key );

		if( values == null ) {
			return Collections.emptyList();
		}

		return values;
	}

	/**
	 * @return The first form value with the given name. null if there's none. A name sent more than once isn't an error: formValuesForKey() has every value.
	 */
	public default String formValueForKey( final String key ) {
		final List<String> values = formValuesForKey( key );

		if( values.isEmpty() ) {
			return null;
		}

		return values.get( 0 );
	}

	/**
	 * @return the request's cookie values
	 */
	public Map<String, List<String>> cookieValues();

	/**
	 * @return The values of the cookies with the given name. An empty list if there are none.
	 */
	public default List<String> cookieValuesForKey( final String key ) {
		Objects.requireNonNull( key );

		final List<String> values = cookieValues().get( key );

		if( values == null ) {
			return Collections.emptyList();
		}

		return values;
	}

	/**
	 * @return The value of the first cookie with the given name. null if there's none. A cookie sent more than once isn't an error (browsers send one per path and domain it was set for): cookieValuesForKey() has every value.
	 */
	public default String cookieValueForKey( final String key ) {
		final List<String> values = cookieValuesForKey( key );

		if( values.isEmpty() ) {
			return null;
		}

		return values.get( 0 );
	}

	public NGContext context();

	/**
	 * @return The sessionID submitted by the client, if any. null if no sessionID was present in the request.
	 */
	public default String _sessionID() {
		return cookieValueForKey( NGApplication.SESSION_ID_COOKIE_NAME );
	}

	/**
	 * @return The network address of the client that sent this request, as reported by the adaptor, or null when the adaptor didn't provide one.
	 */
	public String remoteAddress();

	/**
	 * Invoked by the adaptor once the request has been handled and its response sent, to release whatever the request holds on to (such as an upload's temporary files)
	 */
	public default void _close() {}

	/**
	 * FIXME: Delete. NGRequest really wants to be an immutable interface, awaiting changes in the request/response data model as a whole // Hugi 2026-08-23
	 */
	@Deprecated
	public void setURI( final String uri );

	/**
	 * FIXME: Delete. NGRequest really wants to be an immutable interface, awaiting changes in the request/response data model as a whole // Hugi 2026-08-23
	 */
	@Deprecated
	public void _setContext( NGContext context );
}
