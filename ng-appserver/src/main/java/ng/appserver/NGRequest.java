package ng.appserver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.privates.NGParsedURI;

/**
 * Represents a request entering the system
 */

public class NGRequest extends NGMessage {

	private static final Logger logger = LoggerFactory.getLogger( NGRequest.class );

	/**
	 * Name of the cookie that stores our session ID on the client
	 */
	public static final String SESSION_ID_COOKIE_NAME = "ngsid";

	/**
	 * The request's context
	 */
	private NGContext _context;

	/**
	 * The requests's method
	 */
	private String _method;

	/**
	 * The URI being accessed
	 */
	private String _uri;

	/**
	 * The URI being accessed, wrapped in a nice little API
	 */
	private NGParsedURI _parsedURI;

	/**
	 * The request's form values (a.k.a. query parameters)
	 */
	private Map<String, List<String>> _formValues;

	/**
	 * Values of cookies
	 */
	private Map<String, List<String>> _cookieValues;

	/**
	 * Holds a newly created sessionID
	 */
	private String _newlyCreatedSessionID;

	/**
	 * The request's session
	 */
	private NGSession _session;

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
	}

	/**
	 * @return The requests form values (query parameters)
	 *
	 * FIXME: Form values arent always strings? What about file uploads? (multipart/form-data) // Hugi 2023-03-11
	 */
	public Map<String, List<String>> formValues() {
		return _formValues;
	}

	/**
	 * @return The form values represented by given key/query parameter. Returns an empty list if the form value is not present
	 */
	public List<String> formValuesForKey( final String key ) {
		final List<String> list = formValues().get( key );

		if( list == null ) {
			return Collections.emptyList();
		}

		return list;
	}

	public String formValueForKey( final String key ) {
		final List<String> values = formValuesForKey( key );

		if( values.isEmpty() ) {
			return null; // FIXME: As usual, I'm not happy about returning nulls
		}

		// Fail if multiple form values are present for the same query parameter.
		if( values.size() > 1 ) {
			throw new IllegalStateException( "The request contains %s form values named '%s'. I can only handle one at a time. The values you sent me are (%s).".formatted( values.size(), key, values ) );
		}

		return values.get( 0 );
	}

	/**
	 * Set the request's form values (query parameters)
	 *
	 * FIXME: Same goes for this as the cookieValues. The Map should be populated by the request object, not the adaptor // Hugi 2021-12-31
	 */
	public void _setFormValues( final Map<String, List<String>> formValues ) {
		_formValues = formValues;
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

	/**
	 * @return This request's sessionID. Null if no sessionID is present.
	 */
	public String _sessionID() {
		if( _newlyCreatedSessionID != null ) {
			return _newlyCreatedSessionID;
		}

		return _sessionIDFromCookie();
	}

	/**
	 * @return An ID for an existing sessionID, if one was submitted by the client, null if the client submitted no session ID
	 */
	private String _sessionIDFromCookie() {
		return cookieValueForKey( SESSION_ID_COOKIE_NAME );
	}

	/**
	 * @return This context's session, creating a session if none is present.
	 */
	public NGSession session() {
		if( _session == null ) {
			// OK, we have no session. First, let's see if the request has some session information, so we can restore an existing session
			if( _sessionIDFromCookie() != null ) {
				_session = NGApplication.application().sessionStore().checkoutSessionWithID( _sessionIDFromCookie() );

				// No session found, we enter the emergency phase
				// FIXME: We need to handle the case of a non-existent session better // Hugi 2023-01-10
				if( _session == null ) {
					logger.warn( "No session found with id '{}'", _sessionIDFromCookie() );
					throw new NGSessionRestorationException( this );
				}
			}
			else {
				_session = NGApplication.application().createSessionForRequest( this );
				_newlyCreatedSessionID = _session.sessionID();
				NGApplication.application().sessionStore().storeSession( _session );
			}
		}

		return _session;
	}

	/**
	 * @return This context's session, or null if no session is present.
	 *
	 * FIXME: This currently really only checks if session() has been invoked. We probably need to do a little deeper checking than this // Hugi 2023-01-07
	 */
	public NGSession existingSession() {
		return _session;
	}

	/**
	 * @return True if this context has an existing session
	 */
	public boolean hasSession() {
		return existingSession() != null;
	}

	/**
	 * @return A map of cookie values from the request.
	 */
	public Map<String, List<String>> cookieValues() {
		return _cookieValues;
	}

	/**
	 * FIXME: Don't like having this exposed. Cookie header deserialization should happen in NGRequest instead of in the adaptor // Hugi 20201-12-30
	 */
	public void _setCookieValues( Map<String, List<String>> cookieValues ) {
		_cookieValues = cookieValues;
	}

	/**
	 * @return The values of the named cookie
	 */
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
	public NGContext context() {
		if( _context == null ) {
			_context = NGApplication.application().createContextForRequest( this );
		}

		return _context;
	}

	public void setContext( NGContext context ) {
		_context = context;
	}

	/**
	 * FIXME: This method needs to be thread safe // Hugi 2021-11-28
	 */
	public NGParsedURI parsedURI() {
		if( _parsedURI == null ) {
			_parsedURI = NGParsedURI.of( _uri );
		}

		return _parsedURI;
	}

	@Override
	public String toString() {
		return "NGRequest [_method=" + _method + ", _uri=" + _uri + ", _parsedURI=" + _parsedURI + ", _formValues=" + _formValues + ", _cookieValues=" + _cookieValues + ", _sessionID=" + _newlyCreatedSessionID + ", _session=" + _session + "]";
	}
}