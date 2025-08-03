package ng.appserver.resources;

import java.io.IOException;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for reading resources
 *
 * Consumers rarely use this class directly and should usually go through NGResourceManager instead, which handles caching and other general resource management
 */

public class NGResourceLoader {

	private Map<String, Map<ResourceType, List<ResourceSource>>> _allResourceSources = new ConcurrentHashMap<>();

	public void addResourceSource( final String namespace, final ResourceType resourceType, final ResourceSource resourceSource ) {
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( resourceType );
		Objects.requireNonNull( resourceSource );

		_allResourceSources
				.computeIfAbsent( namespace, _unused -> new ConcurrentHashMap<>() )
				.computeIfAbsent( resourceType, _unused -> new ArrayList<>() )
				.add( resourceSource );
	}

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 *
	 * FIXME: This method is badly named on purpose until we find the appropriate final name // Hugi 2024-06-22
	 */
	public Optional<NGResource> obtainResource( final String namespace, final ResourceType resourceType, String resourcePath ) {
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( resourceType );
		Objects.requireNonNull( resourcePath );

		final Map<ResourceType, List<ResourceSource>> sourceMapForNamespace = _allResourceSources.get( namespace );

		if( sourceMapForNamespace == null ) {
			return Optional.empty();
		}

		final List<ResourceSource> sourceListForType = sourceMapForNamespace.get( resourceType );

		if( sourceListForType == null ) {
			return Optional.empty();
		}

		// Since we don't have the concept of "relative paths", we can always assume an absolute path
		// (meaning we can remove a preceding slash as we always navigate from the root)
		// FIXME: While allowing paths with and without preceding slashes may be nice, it might be *nicer* to standardize a practice of either-or // Hugi 2024-05-25
		if( resourcePath.startsWith( "/" ) ) {
			resourcePath = resourcePath.substring( 1 );
		}

		for( ResourceSource source : sourceListForType ) {
			final Optional<NGResource> result = source.resourceWithPath( resourcePath );

			// FIXME: We should (optionally?) allow iterating through all registered sources to check for duplicates // Hugi 2024-06-14
			if( result.isPresent() ) {
				return result;
			}
		}

		return Optional.empty();
	}

	/**
	 * @return Every namespace registered with the loader
	 */
	public Set<String> namespaces() {
		return _allResourceSources.keySet();
	}

	/**
	 * Represents a source of resources of any type
	 */
	public interface ResourceSource {

		public Optional<NGResource> resourceWithPath( String resourcePath );
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
		public Optional<NGResource> resourceWithPath( String resourcePath ) {
			Objects.requireNonNull( resourcePath );

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

			return Optional.of( NGResource.of( resourceURL ) );
		}

		/**
		 * @return The path to the named resource
		 */
		private String pathWithPrefix( String resourcePath ) {
			return "/" + _basePath + "/" + resourcePath;
		}
	}

	/**
	 * Handles loading of resources from a file system directory
	 */
	public static class FileSystemDirectoryResourceSource implements ResourceSource {

		/**
		 * The directory we're going to locate resources in
		 */
		private final Path _basePath;

		public FileSystemDirectoryResourceSource( final Path basePath ) {
			Objects.requireNonNull( basePath );
			_basePath = basePath;
		}

		@Override
		public Optional<NGResource> resourceWithPath( String resourcePath ) {
			Objects.requireNonNull( resourcePath );

			// The path to the actual file on disk
			final Path filePath = _basePath.resolve( resourcePath );
			return Optional.of( NGResource.of( () -> Files.newInputStream( filePath ) ) );
		}
	}
}