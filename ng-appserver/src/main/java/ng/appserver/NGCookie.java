package ng.appserver;

/**
 * Wraps a cookie
 *
 * // FIXME: We need to decide how to handle timeouts // Hugi 2022-01-01
 * // FIXME: Determine sensible defaults on cookie construction // Hugi 2022-01-01
 */

public class NGCookie {

	private String _name;
	private String _value;
	private String _path;
	private String _domain;
	private String _comment;
	private int _maxAge;
	private boolean _isSecure;
	private boolean _isHttpOnly;

	public NGCookie( final String name, final String value ) {
		setName( name );
		setValue( value );
	}

	public String name() {
		return _name;
	}

	public void setName( String name ) {
		_name = name;
	}

	public String value() {
		return _value;
	}

	public void setValue( String value ) {
		_value = value;
	}

	public String path() {
		return _path;
	}

	public void setPath( String path ) {
		_path = path;
	}

	public String domain() {
		return _domain;
	}

	public void setDomain( String domain ) {
		_domain = domain;
	}

	public String comment() {
		return _comment;
	}

	public void setComment( String comment ) {
		_comment = comment;
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

	public int maxAge() {
		return _maxAge;
	}

	public void setMaxAge( int maxAge ) {
		_maxAge = maxAge;
	}
}