package ng.appserver.http;

import java.util.Objects;

/**
 * A cookie. Yum!
 */

public class NGCookie {

	private String _name;
	private String _value;
	private String _domain;
	private String _path;
	private Long _maxAge;
	private boolean _isSecure;
	private boolean _isHttpOnly;
	private String _sameSite; // Strict,Lax,None

	public NGCookie( String name, String value, String domain, String path, Long maxAge, boolean isSecure, boolean isHttpOnly, String sameSite ) {
		Objects.requireNonNull( name, "A cookie's [name] must not be null" );
		Objects.requireNonNull( value, "A cookie's [value] must not be null" );

		// Browsers reject SameSite=None cookies that aren't Secure, so failing at construction beats failing silently in the user's cookie jar
		if( "None".equals( sameSite ) && !isSecure ) {
			throw new IllegalArgumentException( "A cookie with SameSite=None must also be secure (browsers reject the combination otherwise)" );
		}

		_name = name;
		_value = value;
		_domain = domain;
		_path = path;
		_maxAge = maxAge;
		_isSecure = isSecure;
		_isHttpOnly = isHttpOnly;
		_sameSite = sameSite;
	}

	public NGCookie( final String name, final String value, final Long maxAge ) {
		this( name, value, null, "/", maxAge, false, true, "Lax" );
	}

	public NGCookie( final String name, final String value ) {
		this( name, value, null );
	}

	public String name() {
		return _name;
	}

	public String value() {
		return _value;
	}

	public String domain() {
		return _domain;
	}

	public String path() {
		return _path;
	}

	public void setPath( String path ) {
		_path = path;
	}

	public Long maxAge() {
		return _maxAge;
	}

	public void setMaxAge( Long maxAge ) {
		_maxAge = maxAge;
	}

	public boolean isSecure() {
		return _isSecure;
	}

	public void setSecure( boolean isSecure ) {
		_isSecure = isSecure;
	}

	public boolean isHttpOnly() {
		return _isHttpOnly;
	}

	public void setHttpOnly( boolean isHttpOnly ) {
		_isHttpOnly = isHttpOnly;
	}

	public String sameSite() {
		return _sameSite;
	}

	public void setSameSite( String value ) {
		_sameSite = value;
	}
}