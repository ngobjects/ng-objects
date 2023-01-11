package ng.appserver;

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
	 * The URI being acessed
	 */
	private String _uri;

	/**
	 * The URI being acessed, wrapped in a nice little API
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
	 * FIXME: This is intended as a temporary placeholder. WIP.
	 */
	private String _sessionID;

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

	public Map<String, List<String>> formValues() {
		return _formValues;
	}

	/**
	 * FIXME: Same goes for this as the cookieValues. The Map should be populated by the request object, not the adaptor // Hugi 2021-12-31
	 */
	public void _setFormValues( Map<String, List<String>> formValues ) {
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
	 * FIXME: WIP
	 */
	public String _sessionID() {
		if( _sessionID != null ) {
			return _sessionID;
		}

		return _extractSessionID();
	}

	/**
	 * FIXME: WIP
	 */
	public void _setSessionID( String sessionID ) {
		_sessionID = sessionID;
	}

	/**
	 * @return An ID for an existing sessionID, if one was submitted by the client, null if the client submitted no session ID
	 */
	public String _extractSessionID() {
		return cookieValueForKey( SESSION_ID_COOKIE_NAME );
	}

	/**
	 * @return This context's session, creating a session if none is present.
	 */
	public NGSession session() {
		if( _session == null ) {
			// OK, we have no session. First, let's see if the request has some session information, so we can restore an existing session
			if( _extractSessionID() != null ) {
				_session = NGApplication.application().sessionStore().checkoutSessionWithID( _extractSessionID() );

				// No session found, we enter the emergency phase
				if( _session == null ) {
					logger.warn( "No session found with id '{}'", _extractSessionID() );
					throw new NGSessionRestorationException( this );
					// FIXME: We need to handle the case of a non-existent session better // Hugi 2023-01-10
					//					_session = NGSession.createSession();
					//					_setSessionID( _session.sessionID() );
					//					NGApplication.application().sessionStore().storeSession( _session );
				}
			}
			else {
				_session = NGSession.createSession();
				_setSessionID( _session.sessionID() );
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
		return cookieValues().get( key );
	}

	/**
	 * @return The value of the named cookie if there's only one cookie with that name.
	 * @throws IllegalArgumentException If there are many cookies with the given key
	 */
	public String cookieValueForKey( final String key ) {
		List<String> values = cookieValuesForKey( key );

		// FIXME: Ugh... (vomit) // Hugi 2023-01-10
		if( values == null ) {
			return null;
		}

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
		return "NGRequest [_context=" + _context + ", _method=" + _method + ", _uri=" + _uri + ", _parsedURI=" + _parsedURI + ", _formValues=" + _formValues + ", _cookieValues=" + _cookieValues + ", _sessionID=" + _sessionID + ", _session=" + _session + "]";
	}
}