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

	private static final String SESSION_ID_COOKIE_NAME = "wosid";

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
	 *
	 */
	public String _extractSessionID() {
		logger.warn( "Returning fake session ID" );
		return "fake-session-id";
		//		return cookieValueForKey( SESSION_ID_COOKIE_NAME );
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

		if( values.size() == 0 ) {
			return null;
		}

		if( values.size() > 1 ) {
			throw new IllegalArgumentException( "There are more than one cookie named " + key );
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
}