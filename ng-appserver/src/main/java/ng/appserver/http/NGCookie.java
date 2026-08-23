package ng.appserver.http;

/**
 * A cookie. Yum!
 */

public class NGCookie {

	private String _name;
	private String _value;
	private String _domain;
	private String _path;
	private Integer _maxAge;
	private boolean _isSecure;
	private boolean _isHttpOnly;
	private String _sameSite; // Strict,Lax,None

	public NGCookie( final String name, final String value ) {
		_name = name;
		_value = value;
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

	public Integer maxAge() {
		return _maxAge;
	}

	public void setMaxAge( Integer maxAge ) {
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