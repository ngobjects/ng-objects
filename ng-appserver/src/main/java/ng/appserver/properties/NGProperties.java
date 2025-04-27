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
 * FIXME: You probably need a "configuration set" to group certain properties together. For example, you may need to flip the dev machine to talk to the production DB. Or flip a single web service to speak to production, both of which may need multiple properties // Hugi 2025-04-20
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
	 */
	public void addAndReadCommandLineArguments( final PropertiesSource source ) {
		_sources.add( source );
		_readAllSources();
	}

	/**
	 * Add a property source before the command line arguments, i.e. it's properties will overridde all previously inserted resource sources, except the CLI arguments which always maintain priority
	 */
	public void addAndReadSource( final PropertiesSource source ) {
		// We assume that the sources already contains one object, the command line arguments
		final int index = _sources.size() - 1;
		_sources.add( index, source );
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
				logger.debug( "Property resource not found {}::{}", _namespace, _resourcePath );
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
			final Map<String, String> properties = new HashMap<>();

			for( int i = 0; i < _argv.length; i++ ) {
				String currentArgument = _argv[i];

				if( currentArgument.startsWith( "-X" ) ) {
					// Notify the user he's probably passing an argument in the wrong place
					logger.warn( "IGNORED ARGUMENT '%s'. Arguments starting with -X are meant for the JVM".formatted( currentArgument ) );
				}
				else if( currentArgument.startsWith( "-D" ) ) {
					// Java-style arguments are passed on to both System.properties() and NG's properties
					final String[] keyValuePair = currentArgument.split( "=" ); // FIXME: Look into treatment of missong or multiple equals signs // Hugi 2025-04-27
					final String key = keyValuePair[0].substring( 2 );
					final String value = keyValuePair[1];
					System.setProperty( key, value );
					properties.put( key, value ); // FIXME: It's actually questionable whether we want to add java system properties to our main properties or just to System.properties() // Hugi 2025-04-27
				}
				else if( currentArgument.startsWith( "-" ) ) {
					// "our style arguments" are passed to NG only
					currentArgument = currentArgument.substring( 1 );

					// Since property/value are separated by a space in this style, they count as two arguments in the originating array.
					// So, proceed to the next argument for treating it as as the value.
					i++;
					final String value = _argv[i];
					properties.put( currentArgument, value );
				}
				else {
					// FIXME:
					// We're entirely ignoring non-prefixed arguments.
					// Since this is _probably_ some sort of a user error,
					// we're probably going to want to throw or notify the user in some other nice way.
					// Hugi 2025-04-27
					logger.warn( "IGNORED ARGUMENT '%s'. Prefix your argument with a hyphen if you want it treated as an NG property: ".formatted( currentArgument ) );
				}
			}

			return properties;
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

	private NGPropertiesDeprecated _d;

	/**
	 * FIXME: TEmporary bridge to old properties
	 */
	@Deprecated
	public NGPropertiesDeprecated d() {
		if( _d == null ) {
			_d = new NGPropertiesDeprecated( this );
		}

		return _d;
	}
}