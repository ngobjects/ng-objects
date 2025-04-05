package ng.appserver;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
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
			return null;
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

	/**
	 * @return the request's URL
	 * FIXME: This method should probably be called "url" rather than "uri" // Hugi 2024-06-29
	 */
	public String uri() {
		return _uri;
	}

	/**
	 * FIXME: This should really not be settable, NGRequest wants to be immutable // Hugi 2024-08-10
	 */
	public void setURI( final String uri ) {
		_uri = uri;
	}

	/**
	 * @return The request's method
	 */
	public String method() {
		return _method;
	}

	/**
	 * FIXME: This should really not be settable, NGRequest wants to be immutable // Hugi 2024-08-10
	 */
	public void setMethod( final String method ) {
		_method = method;
	}

	/**
	 * @return This request's sessionID. Null if no sessionID is present.
	 */
	public String _sessionID() {
		if( _session != null ) {
			return _session.sessionID();
		}

		return _sessionIDFromCookie();
	}

	/**
	 * @return The sessionID submitted by the client, if any. null if no sessionID was present in the request.
	 */
	private String _sessionIDFromCookie() {
		return cookieValueForKey( SESSION_ID_COOKIE_NAME );
	}

	/**
	 * @return This request's session, creating a new session if no session is present. Throws NGSessionRestorationException if a sessionID is present but no corresponding session is found.
	 */
	public NGSession session() {
		return _session( true, true );
	}

	/**
	 * @return This request's session, null if no session present. Throws NGSessionRestorationException if a sessionID is present but no corresponding session is found.
	 *
	 * FIXME: We might actually want to throw on a missing sessionID here. Decide soon, otherwise this will get really annoying upon a change // Hugi 2024-10-08
	 */
	public NGSession existingSession() {
		return _session( false, false );
	}

	/**
	 * @return A session for this request.
	 *
	 * @param createIfMissing If true, we'll create a new session if no session (or sessionID) is present.
	 * @param throwIfIDPresentButNoCorrespondingSessionFound If true, will throw an NGSessionRestorationException if a sessionID is present but no corresponding session is found.
	 */
	private NGSession _session( boolean createIfMissing, boolean throwIfIDPresentButNoCorrespondingSessionFound ) {
		if( _session == null ) {
			// OK, we have no session. Check the request for a sessionID and see if we have one to restore.
			if( _sessionIDFromCookie() != null ) {
				_session = NGApplication.application().sessionStore().checkoutSessionWithID( _sessionIDFromCookie() );

				// No session found, loudly notify the user
				if( _session == null && throwIfIDPresentButNoCorrespondingSessionFound ) {
					logger.debug( "No session found with id '{}'", _sessionIDFromCookie() );
					throw new NGSessionRestorationException( this );
				}
			}
			else {
				if( createIfMissing ) {
					_session = NGApplication.application().createSessionForRequest( this );
					NGApplication.application().sessionStore().storeSession( _session );
				}
			}
		}

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

	/**
	 * FIXME: This is temporary handling of multipart file uploads. Still thinking on the final design of this // Hugi 2025-04-05
	 */
	private final Map<String, UploadedFile> _uploadedFiles = new HashMap<>();

	public record UploadedFile( String name, String contentType, InputStream stream, long length ) {}

	public Map<String, UploadedFile> _uploadedFiles() {
		return _uploadedFiles;
	}

	@Override
	public String toString() {
		return "NGRequest [_method=" + _method + ", _uri=" + _uri + ", _headers=" + headers() + ", _formValues=" + _formValues + ", _cookieValues=" + _cookieValues + ", _session=" + _session + "]";
	}
}