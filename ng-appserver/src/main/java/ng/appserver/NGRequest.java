package ng.appserver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a request entering the system
 */

public class NGRequest extends NGMessage {

	private static final String SESSION_ID_COOKIE_NAME = "wosid";

	/**
	 * FIXME: Make sure we're initializing this correctly during the request's lifecycle
	 */
	private NGContext _context;

	/**
	 * FIXME: Shouldn't this really be an enum, or do we need to support arbitrary methods?
	 */
	private String _method;

	/**
	 * The URI being acessed
	 */
	private String _uri;

	/**
	 * FIXME: Do we want to store this? Does parsing happen at the adaptor level or here?
	 */
	private final Map<String, List<String>> _formValues;

	public Map<String, List<String>> formValues() {
		return _formValues;
	}

	public String uri() {
		return _uri;
	}

	public void setURI( final String uri ) {
		_uri = uri;
	}

	public String method() {
		return _method;
	}

	public void setMethod( final String method ) {
		_method = method;
	}

	public NGRequest( final String method, final String uri, final String httpVersion, final Map<String, List<String>> headers, final byte[] contentBytes ) {
		Objects.requireNonNull( method );
		Objects.requireNonNull( uri );
		Objects.requireNonNull( httpVersion );
		Objects.requireNonNull( headers );
		Objects.requireNonNull( contentBytes );

		setMethod( method );
		setURI( uri );
		setHttpVersion( httpVersion );
		setHeaders( headers );
		setContentBytes( contentBytes );

		// FIXME: We're going to have to do some work here (or rather, not here, but lazily done in formValues()) to parse the form values based on the request's type/encoding etc.
		_formValues = Collections.emptyMap();
	}

	/**
	 *
	 */
	public String _extractSessionID() {
		return cookieValueForKey( SESSION_ID_COOKIE_NAME );
	}

	/**
	 * FIXME: Inefficient looping
	 */
	private String cookieValueForKey( String string ) {

		for( NGCookie cookie : cookies() ) {
			if( cookie.name().equals( SESSION_ID_COOKIE_NAME ) ) {
				return cookie.value();
			}
		}

		return null;
	}

	public NGContext context() {
		return _context;
	}
}