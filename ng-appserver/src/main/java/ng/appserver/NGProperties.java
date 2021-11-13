package ng.appserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NGProperties {

	private Map<String, String> _map;

	public NGProperties( final String[] args ) {
		initWithArgs( args );
	}

	public void initWithArgs( final String[] args ) {
		Objects.requireNonNull( args );

		System.out.println( "Initing properties with args: " + Arrays.asList( args ) );
		_map = new HashMap<>();

		for( int i = 0; i < args.length; i = i + 2 ) {
			String key = args[i];

			if( key.startsWith( "-" ) ) {
				key = key.substring( 1 );
			}

			final String value = args[i + 1];
			_map.put( key, value );
		}
		System.out.println( "Parsed properties: " + _map );
	}

	/**
	 * @return The named property
	 *
	 * FIXME: Currently returns null if the property does not exist. Might want to return an Optional.
	 */
	public String get( final String key ) {
		return _map.get( key );
	}

	/**
	 * @return The named integer property
	 *
	 * FIXME: Currently returns null if the property does not exist. Might want to return an Optional.
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

		ArrayList<String> keys = new ArrayList<>( _map.keySet() );
		Collections.sort( keys );

		for( String key : keys ) {
			String value = _map.get( key );
			b.append( String.format( "'%s':'%s'\n", key, value ) );
		}

		return b.toString();
	}
}