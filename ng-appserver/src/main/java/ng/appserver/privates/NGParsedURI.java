package ng.appserver.privates;

import java.util.Objects;
import java.util.Optional;

/**
 * Wraps URL paths for easy access to its components.
 *
 * - only stores the path (no hostname, no protocol) - does not differentiate between relative and absolute urls
 */

public class NGParsedURI {

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
	 * Instances are constructed using the of() method.
	 */
	private NGParsedURI( final String sourceURL ) {
		Objects.requireNonNull( sourceURL );

		String parsedURL = sourceURL;

		if( parsedURL.startsWith( "/" ) ) {
			parsedURL = parsedURL.substring( 1 );
		}

		if( parsedURL.endsWith( "/" ) ) {
			parsedURL = parsedURL.substring( 0, parsedURL.length() - 1 );
		}

		_parsedURL = parsedURL;
		_sourceURL = sourceURL;
	}

	public static NGParsedURI of( final String sourceURL ) {
		return new NGParsedURI( sourceURL );
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
			// FIXME: This feels a bit hacky, look into it a little better later // Hugi 2021-12-29
			if( "".equals( _parsedURL ) ) {
				_pathElements = new String[0];
			}
			else {
				_pathElements = _parsedURL.split( "/" );
			}
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

	/**
	 * FIXME: Deprecated, delete
	 */
	@Deprecated
	public Optional<String> elementAt( final int i ) {

		if( i >= pathElements().length ) {
			return Optional.empty();
		}

		return Optional.of( pathElements()[i] );
	}
}