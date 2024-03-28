package ng.appserver.privates;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for reading resources
 *
 * FIXME: Consumers should really never be going through this class directly. Resource providers should be registered with NGResourceManager and resources then loaded from there // Hugi 2023-07-08
 */

public class NGResourceLoader {

	public enum StandardNamespace {
		App( "app" );

		private String _identifier;

		StandardNamespace( final String identifier ) {
			_identifier = identifier;
		}

		public String identifier() {
			return _identifier;
		}
	}

	private static Map<ResourceType, List<ResourceSource>> _resourceSources = new ConcurrentHashMap<>();

	static {
		addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.App, new JavaClasspathResourceSource( "app-resources" ) );
		addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.WebServer, new JavaClasspathResourceSource( "webserver-resources" ) );
		addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.Public, new JavaClasspathResourceSource( "public" ) );
		addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.ComponentTemplate, new JavaClasspathResourceSource( "components" ) );
	}

	private static void addResourceSource( final String namespace, final ResourceType type, final ResourceSource source ) {
		List<ResourceSource> sources = _resourceSources.get( type );

		if( sources == null ) {
			sources = new ArrayList<>();
			_resourceSources.put( type, sources );
		}

		sources.add( source );
		_resourceSources.put( type, sources );
	}

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 */
	private static Optional<byte[]> readResource( final ResourceType type, final String resourcePath ) {
		Objects.requireNonNull( type );
		Objects.requireNonNull( resourcePath );

		final List<ResourceSource> list = _resourceSources.get( type );

		// FIXME: Ugly null check. Perhaps just add an empty list to sources instead for each resource type at startup? // Hugi 2023-11-09
		if( list == null ) {
			return Optional.empty();
		}

		for( ResourceSource source : list ) {
			final Optional<byte[]> result = source.bytesForResourceWithPath( resourcePath );

			// CHECKME: Should we rather iterate through all registered sources to check for duplicates?
			if( result.isPresent() ) {
				return result;
			}
		}

		return Optional.empty();
	}

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 */
	public static Optional<byte[]> bytesForPublicResource( final String resourcePath ) {
		return readResource( StandardResourceType.Public, resourcePath );
	}

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 */
	public static Optional<byte[]> bytesForWebserverResource( final String resourcePath ) {
		return readResource( StandardResourceType.WebServer, resourcePath );
	}

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 */
	public static Optional<byte[]> bytesForAppResource( final String resourcePath ) {
		return readResource( StandardResourceType.App, resourcePath );
	}

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 */
	public static Optional<byte[]> bytesForComponentResource( final String resourcePath ) {
		return readResource( StandardResourceType.ComponentTemplate, resourcePath );
	}

	/**
	 * Represents a source of resources of any type
	 */
	public interface ResourceSource {

		public default Optional<byte[]> bytesForResourceWithPath( String resourcePath ) {
			final Optional<InputStream> iso = inputStreamForResourceWithPath( resourcePath );

			if( iso.isEmpty() ) {
				return Optional.empty();
			}

			try( InputStream is = iso.get()) {
				return Optional.of( is.readAllBytes() );
			}
			catch( IOException e ) {
				throw new UncheckedIOException( e );
			}
		}

		public Optional<InputStream> inputStreamForResourceWithPath( String resourcePath );
	}

	/**
	 * Wraps loading of resources from the classpath
	 */
	public static class JavaClasspathResourceSource implements ResourceSource {

		private static final Logger logger = LoggerFactory.getLogger( JavaClasspathResourceSource.class );

		/**
		 * Classpath prefix
		 */
		private final String _basePath;

		public JavaClasspathResourceSource( final String basePath ) {
			Objects.requireNonNull( basePath );
			_basePath = basePath;
		}

		@Override
		public Optional<InputStream> inputStreamForResourceWithPath( String resourcePath ) {
			Objects.requireNonNull( resourcePath );

			// FIXME:
			// Initially added to fix up some mess with public resources. Needs to be fixed at the origin site.
			// So, the problem here is that resource paths really don't differentiate between "absolute" and "relative".
			// Technically, starting the path with a slash is the right thing to do (since it's absolute and points to the root).
			// We need to decide "the standard way" here and settle on it. Allowing both (with and without slash) feels sloppy // Hugi 2024-03-28
			if( resourcePath.startsWith( "/" ) ) {
				resourcePath = resourcePath.substring( 1 );
			}

			logger.debug( "Reading resource {} ", resourcePath );

			resourcePath = pathWithPrefix( resourcePath );

			logger.debug( "Reading resourcePath {} ", resourcePath );

			URL resourceURL = null;

			try {
				// Our default functionality uses a preceding slash a-la loading a resource by name from the class. ClassLoader doesn't want the preceding slash.
				final String resourcePathForClassLoader = resourcePath.substring( 1 );

				// We're using this method to locate resources, in case there's more than one resource on the classpath with the same name
				final Enumeration<URL> resources = JavaClasspathResourceSource.class.getClassLoader().getResources( resourcePathForClassLoader );

				// We iterate through the resources and pick the first one to return. Then we log a warning if there are more resources with the same name.
				while( resources.hasMoreElements() ) {
					final URL currentURL = resources.nextElement();

					if( resourceURL == null ) {
						resourceURL = currentURL;
					}
					else {
						logger.warn( "Duplicate resource found for path '{}'. I'm using '{}' and ignoring '{}'", resourcePath, resourceURL, currentURL );
					}
				}
			}
			catch( IOException ioException ) {
				throw new UncheckedIOException( ioException );
			}

			if( resourceURL == null ) {
				return Optional.empty();
			}

			try {
				return Optional.of( resourceURL.openStream() );
			}
			catch( final IOException ioException ) {
				throw new UncheckedIOException( ioException );
			}
		}

		/**
		 * @return The path to the named resource
		 */
		private String pathWithPrefix( String resourcePath ) {
			return "/" + _basePath + "/" + resourcePath;
		}
	}

	/**
	 * Wraps loading of resources from a
	 */
	public static class FileSystemDirectoryResourceSource implements ResourceSource {

		private static final Logger logger = LoggerFactory.getLogger( FileSystemDirectoryResourceSource.class );

		/**
		 * The directory we're going to locate resources in
		 */
		private final Path _basePath;

		public FileSystemDirectoryResourceSource( final Path basePath ) {
			Objects.requireNonNull( basePath );
			_basePath = basePath;
		}

		@Override
		public Optional<InputStream> inputStreamForResourceWithPath( String resourcePath ) {
			Objects.requireNonNull( resourcePath );

			logger.debug( "Reading resource {} ", resourcePath );

			try {
				// The path to the actual file on disk
				final Path filePath = _basePath.resolve( resourcePath );
				return Optional.of( Files.newInputStream( filePath ) );
			}
			catch( IOException e1 ) {
				throw new RuntimeException( e1 );
			}
		}
	}
}