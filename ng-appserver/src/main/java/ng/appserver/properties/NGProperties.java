package ng.appserver.properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.resources.NGResource;

/**
 * Handles properties loading
 *
 * FIXME: A decision needs to be made on if properties should return an Optional // Hugi 2021-11-21
 * FIXME: Mark the origin of properties // Hugi 2021-11-21
 * FIXME: Properties need to get updated when a loaded Properties file is changed // Hugi 2023-02-22
 * FIXME: Properties should be typesafe // Hugi 2023-07-15
 * FIXME: Properties need default values // Hugi 2023-07-15
 * FIXME: Properties need watching for changes (allowing us to trigger logic when properties change, like switching loggers, flushing caches etc.) // Hugi 2024-07-10
 * FIXME: Property files loaded from resource/file need a preview feature in the control UI // Hugi 2024-07-10
 * FIXME: Configuration for different deployment modes should be previewable as well in the UI // Hugi 2024-07-10
 */

public class NGProperties {

	private static final Logger logger = LoggerFactory.getLogger( NGProperties.class );

	/**
	 * Keeps track of the final resolved properties.
	 */
	private final Map<String, String> _allProperties;

	/**
	 * List of sources of properties
	 */
	private final List<PropertiesSource> _sources;

	public NGProperties() {
		_allProperties = new ConcurrentHashMap<>();
		_sources = new ArrayList<>();
	}

	/**
	 * @return a list of sources properties get loaded from. The order of the list determines which property "wins" when properties are overridden.
	 */
	public List<PropertiesSource> sources() {
		return _sources;
	}

	/**
	 * Set the named properties
	 */
	private void putAll( final Map<String, String> properties ) {
		_allProperties.putAll( properties );
	}

	/**
	 * @return A list of all configured property keys
	 */
	public Collection<String> allKeys() {
		return _allProperties.keySet();
	}

	/**
	 * Add a property source that will override all previously inserted resource sources
	 *
	 * FIXME: This will eventually be refactored, just starting out the work on prioritizing resource sources // Hugi 2024-07-09
	 */
	public void addAndReadSourceHighestPriority( final PropertiesSource source ) {
		_sources.add( source );
		_readAllSources();
	}

	/**
	 * Add a property source at the very back, i.e. it's properties will be overridden by all previously inserted resource sources
	 *
	 * FIXME: This will eventually be refactored, just starting out the work on prioritizing resource sources // Hugi 2024-07-09
	 */
	public void addAndReadSourceLowestPriority( final PropertiesSource source ) {
		_sources.add( 0, source );
		_readAllSources();
	}

	/**
	 * Read all the property sources and apply their properties
	 */
	private void _readAllSources() {
		_allProperties.clear();

		for( PropertiesSource propertiesSource : sources() ) {
			putAll( propertiesSource.readAll() );
		}
	}

	public static interface PropertiesSource {

		public Map<String, String> readAll();

		public String description();
	}

	/**
	 * FIXME: Finish implementation of this // This should contain the properties set in sources // Hugi 2023-08-05
	 */
	public static class PropertiesSourceCode implements PropertiesSource {

		private Map<String, String> _properties;

		@Override
		public Map<String, String> readAll() {
			return _properties;
		}

		@Override
		public String description() {
			return "Properties set in source";
		}

		@Override
		public String toString() {
			return "PropertiesSourceCode [_properties=" + _properties + "]";
		}
	}

	/**
	 * Loads properties from a named resource
	 */
	public static class PropertiesSourceResource implements PropertiesSource {

		private final String _namespace;
		private final String _resourcePath;

		public PropertiesSourceResource( final String namespace, final String resourcePath ) {
			_namespace = namespace;
			_resourcePath = resourcePath;
		}

		public String namespace() {
			return _resourcePath;
		}

		public String resourcePath() {
			return _resourcePath;
		}

		@Override
		public Map<String, String> readAll() {
			final Optional<NGResource> propertyBytes = NGApplication.application().resourceManager().obtainAppResource( _namespace, _resourcePath );

			if( !propertyBytes.isPresent() ) {
				logger.warn( "No default properties file found {}::{}", _namespace, _resourcePath );
				return Collections.emptyMap();
			}

			try {
				final Properties p = new Properties();

				try( final InputStream is = propertyBytes.get().inputStream()) {
					p.load( is );
				}

				return (Map)p;
			}
			catch( IOException e ) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public String description() {
			return "Resource file %s:%s".formatted( namespace(), resourcePath() );
		}

		@Override
		public String toString() {
			return "PropertiesSourceResource [_namespace=" + _namespace + ", _resourcePath=" + _resourcePath + "]";
		}
	}

	/**
	 * Generates properties by parsing the arguments passed on to the Application's startup script
	 */
	public static class PropertiesSourceArguments implements PropertiesSource {

		private final String[] _argv;

		public PropertiesSourceArguments( final String[] argv ) {
			_argv = argv;
		}

		public String[] arguments() {
			return _argv;
		}

		@Override
		public Map<String, String> readAll() {
			final Map<String, String> m = new HashMap<>();

			for( int i = 0; i < _argv.length; i = i + 2 ) {
				String key = _argv[i];

				if( key.startsWith( "-X" ) ) {
					// JVM argument - ignore
					// FIXME: Not sure about this functionality, experimental // Hugi  2023-04-17
				}
				else if( key.startsWith( "-D" ) ) {
					// Java argument is passed on to System.properties
					// FIXME: This is definitely not the place to set System properties - experimental // Hugi 2023-04-17
					final String[] keyValuePair = key.split( "=" );
					final String realKey = keyValuePair[0].substring( 2 );
					final String realValue = keyValuePair[1];
					System.setProperty( realKey, realValue );
					m.put( realKey, realValue );
				}
				else if( key.startsWith( "-" ) ) {
					// Otherwise this is a standard property
					key = key.substring( 1 );

					final String value = _argv[i + 1];
					m.put( key, value );
				}
			}

			return m;
		}

		@Override
		public String description() {
			return "Properties from command line arguments";
		}

		@Override
		public String toString() {
			return "PropertiesSourceArguments [_argv=" + Arrays.toString( _argv ) + "]";
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
		final List<String> stringKeyPairs = new ArrayList<>();

		final ArrayList<String> keys = new ArrayList<>( _allProperties.keySet() );
		Collections.sort( keys );

		for( String key : keys ) {
			final String value = _allProperties.get( key );
			stringKeyPairs.add( String.format( "'%s':'%s'", key, value ) );
		}

		return String.join( "\n", stringKeyPairs );
	}

	/**
	 * @return true if the application is in development mode.
	 *
	 * FIXME: This needs a more robust and explicit implementation, see issue #6 // Hugi 2021-12-29
	 * FIXME: This definitely needs some caching since it's invoked quite often // Hugi 2024-03-15
	 */
	public boolean isDevelopmentMode() {
		return !propWOMonitorEnabled();
	}

	/**
	 * Defines a property
	 *
	 * FIXME: Finish this concept up (for defined, typesafe properties) // Hugi 2023-08-05
	 */
	public static class Property<E> {
		private String _key;
		private E _value;
		private E _defaultValue;

		public Property( final String key, E value, E defaultValue ) {
			_key = key;
			_value = value;
			_defaultValue = defaultValue;
		}

		public String key() {
			return _key;
		}

		public E value() {
			return _value;
		}

		public E defaultValue() {
			return _defaultValue;
		}
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