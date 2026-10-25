package ng.appserver.http;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGApplication;
import ng.appserver.NGContext;

/**
 * Represents a request entering the system
 *
 * Implementations differ in how they hold the request's body: NGStandardRequest holds it as bytes, NGMultipartRequest as parts. NGRequests.of() creates the one a request's content type calls for.
 */

public interface NGRequest extends NGMessage {

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
