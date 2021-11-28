package ng.appserver.experimental;

/**
 * Wraps URL paths for easy access to its components.
 *
 * - only stores the path (no hostname, no protocol) - does not differentiate between relative and absolute urls
 */

public class WrappedURL {

	/**
	 * The URL string this object was generated from.
	 */
	private String _sourceURL;

	/**
	 * The cleaned up URL
	 */
	private String _parsedURL;

	/**
	 * cached copy of the url's elements
	 */
	private String[] _pathElements;

	/**
	 * Instances are constructed using the create() method.
	 */
	private WrappedURL() {}

	public static WrappedURL create( final String sourceURL ) {
		String parsedURL = sourceURL;

		if( parsedURL == null ) {
			parsedURL = "";
		}

		if( parsedURL.startsWith( "/" ) ) {
			parsedURL = parsedURL.substring( 1 );
		}

		if( parsedURL.endsWith( "/" ) ) {
			parsedURL = parsedURL.substring( 0, parsedURL.length() - 1 );
		}

		WrappedURL object = new WrappedURL();
		object._parsedURL = parsedURL;
		object._sourceURL = sourceURL;
		return object;
	}

	public String sourceURL() {
		return _sourceURL;
	}

	@Override
	public String toString() {
		return _parsedURL;
	}

	private String[] pathElements() {
		if( _pathElements == null ) {
			_pathElements = _parsedURL.split( "/" );
		}

		return _pathElements;
	}

	/**
	 * @return The string value at [index] in the path. defaultValue if [index] does not exist.
	 */
	public String getString( int index, String defaultValue ) {

		if( index > pathElements().length - 1 ) {
			return defaultValue;
		}

		return pathElements()[index];
	}

	/**
	 * @return The string value at [index] in the path. null if [index] does not exist.
	 */
	public String getString( int index ) {
		return getString( index, null );
	}

	/**
	 * @return The integer value at [index] in the path. defaultValue if [index] does not exist.
	 */
	public Integer getInteger( int index, Integer defaultValue ) {
		String value = getString( index );

		if( value == null ) {
			return defaultValue;
		}

		return Integer.valueOf( value );
	}

	/**
	 * @return The integer value at [index] in the path. null if [index] does not exist.
	 */
	public Integer getInteger( int index ) {
		return getInteger( index, null );
	}

	/**
	 * @return The number of elements in the URL
	 */
	public int length() {
		return pathElements().length;
	}

	public String getNamedParameter( final String parameterName ) {
		throw new RuntimeException( "Implement" ); // FIXME: Implement
	}
}