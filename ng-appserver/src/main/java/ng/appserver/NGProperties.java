package ng.appserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.privates.NGUtils;

/**
 * Handles properties loading
 *
 * FIXME: A decision needs to be made on if properties should return an Optional // Hugi 2021-11-21
 * FIXME: We need to cache properties // Hugi 2021-11-21
 * FIXME: Mark the origin of properties // Hugi 2021-11-21
 */

public class NGProperties {

	private static final Logger logger = LoggerFactory.getLogger( NGProperties.class );

	/**
	 * Keeps track of the final resolved properties.
	 */
	private final Map<String, String> _allProperties;

	public NGProperties( final String[] args ) {
		_allProperties = new ConcurrentHashMap<>();
		_allProperties.putAll( loadDefaultProperties() );
		_allProperties.putAll( loadPropertiesFromArgsString( args ) );
	}

	/**
	 * @return Properties passed to the main method as a Map
	 */
	private static Map<String, String> loadPropertiesFromArgsString( final String[] args ) {
		final Map<String, String> m = new HashMap<>();

		for( int i = 0; i < args.length; i = i + 2 ) {
			String key = args[i];

			if( key.startsWith( "-" ) ) {
				key = key.substring( 1 );
			}

			final String value = args[i + 1];
			m.put( key, value );
		}

		return m;
	}

	/**
	 * @return The default properties as a map (loaded from the default Properties file)
	 */
	private Map<String, String> loadDefaultProperties() {
		final Optional<byte[]> propertyBytes = NGUtils.readAppResource( "Properties" );

		if( !propertyBytes.isPresent() ) {
			logger.warn( "No default properties file found" );
			return Collections.emptyMap();
		}

		try {
			final Properties p = new Properties();
			p.load( new ByteArrayInputStream( propertyBytes.get() ) );
			return (Map)p;
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * @return The named property
	 */
	public String get( final String key ) {
		return _allProperties.get( key );
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

		ArrayList<String> keys = new ArrayList<>( _allProperties.keySet() );
		Collections.sort( keys );

		for( String key : keys ) {
			String value = _allProperties.get( key );
			b.append( String.format( "'%s':'%s'\n", key, value ) );
		}

		return b.toString();
	}

	/**
	 * @return true if the application is in development mode.
	 *
	 * FIXME: This needs a more robust and explicit implementation, see issue #6 // Hugi 2021-12-29
	 */
	public boolean isDevelopmentMode() {
		return !propWOMonitorEnabled();
	}

	/**
	 * FIXME: We're creating cover methods for some of the more used properties for now. // Hugi 2021-12-29
	 * This is not the way we it'll be going forward, but it will help with refactoring later (rather than using property name strings)
	 */
	public Integer propWOPort() {
		return getInteger( WOProperties.WOPort.name() );
	}

	public String propWOHost() {
		return get( WOProperties.WOHost.name() );
	}

	public Integer propWOLifebeatDestinationPort() {
		return getInteger( WOProperties.WOLifebeatDestinationPort.name() );
	}

	public Integer propWOLifebeatIntervalInSeconds() {
		return getInteger( WOProperties.WOLifebeatInterval.name() );
	}

	public boolean propWOLifebeatEnabled() {
		return "YES".equals( get( WOProperties.WOLifebeatEnabled.name() ) );
	}

	public boolean propWOMonitorEnabled() {
		return "YES".equals( get( WOProperties.WOMonitorEnabled.name() ) );
	}

	public String propWOApplicationName() {
		return get( WOProperties.WOApplicationName.name() );
	}

	public String propWOOutputPath() {
		return get( WOProperties.WOOutputPath.name() );
	}

	/**
	 * Container for all the old properties from WO
	 */
	public static enum WOProperties {
		// Properties in use
		WOOutputPath,
		WOApplicationName,
		WOMonitorEnabled,
		WOLifebeatEnabled,
		WOLifebeatInterval,
		WOLifebeatDestinationPort,
		WOHost,
		WOPort,

		// Properties currently NOT in use
		NSProjectSearchPath,
		WOAdaptor,
		WOAutoOpenClientApplication,
		WOAutoOpenInBrowser,
		WOCachingEnabled,
		WODebuggingEnabled,
		WOListenQueueSize,
		WONoPause,
		WOSessionTimeOut,
		WOWorkerThreadCount,
		WOWorkerThreadCountMax,
		WOWorkerThreadCountMin
	}
}