package ng.appserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handles properties loading
 *
 * FIXME: A decision needs to be made on if properties should return an Optional // Hugi 2021-11-21
 * FIXME: We need to cache properties // Hugi 2021-11-21
 * FIXME: Mark the origin of properties // Hugi 2021-11-21
 */

public class NGProperties {

	/**
	 * Keeps track of the final resolved properties.
	 */
	private Map<String, String> _resolvedPropertiesMap;

	public NGProperties( final String[] args ) {
		initWithArgs( args );
	}

	public void initWithArgs( final String[] args ) {
		Objects.requireNonNull( args );

		System.out.println( "Initing properties with args: " + Arrays.asList( args ) );
		_resolvedPropertiesMap = new HashMap<>();

		for( int i = 0; i < args.length; i = i + 2 ) {
			String key = args[i];

			if( key.startsWith( "-" ) ) {
				key = key.substring( 1 );
			}

			final String value = args[i + 1];
			_resolvedPropertiesMap.put( key, value );
		}
		System.out.println( "Parsed properties: " + _resolvedPropertiesMap );
	}

	/**
	 * @return The named property
	 */
	public String get( final String key ) {
		return _resolvedPropertiesMap.get( key );
	}

	/**
	 * @return The named integer property
	 */
	public Integer getInteger( final String key ) {
		final String value = get( key );

		if( value == null ) {
			return null;
		}

		return Integer.valueOf( value );
	}

	/**
	 * FIXME: I probably don't want this here. I still feel it's better than exposing the properties map at this stage
	 */
	public String _propertiesMapAsString() {
		StringBuilder b = new StringBuilder();

		ArrayList<String> keys = new ArrayList<>( _resolvedPropertiesMap.keySet() );
		Collections.sort( keys );

		for( String key : keys ) {
			String value = _resolvedPropertiesMap.get( key );
			b.append( String.format( "'%s':'%s'\n", key, value ) );
		}

		return b.toString();
	}
}