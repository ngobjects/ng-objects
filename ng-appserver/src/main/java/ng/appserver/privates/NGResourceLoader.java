package ng.appserver.privates;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for reading resources
 */

public class NGResourceLoader {

	/**
	 * Name of the folder that stores application resources
	 */
	private static final String APP_RESOURCES_FOLDER = "app-resources";

	private static ResourceSource appResourcesSource = new JavaClasspathResourceSource( APP_RESOURCES_FOLDER );

	/**
	 * Name of the folder that stores application resources
	 */
	private static final String WEBSERVER_RESOURCES_FOLDER = "webserver-resources";

	private static ResourceSource webserverResourcesSource = new JavaClasspathResourceSource( WEBSERVER_RESOURCES_FOLDER );

	/**
	 * Name of the folder that stores component templates
	 */
	private static final String COMPONENTS_FOLDER = "components";

	private static ResourceSource componentResourcesSource = new JavaClasspathResourceSource( COMPONENTS_FOLDER );

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 */
	public static Optional<byte[]> readWebserverResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );
		return webserverResourcesSource.bytesForResourceWithPath( resourcePath );
	}

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 */
	public static Optional<byte[]> readAppResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );
		return appResourcesSource.bytesForResourceWithPath( resourcePath );
	}

	public static Optional<byte[]> readComponentResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );
		return componentResourcesSource.bytesForResourceWithPath( resourcePath );
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

			logger.debug( "Reading resource {} ", resourcePath );

			resourcePath = pathWithPrefix( resourcePath );

			logger.debug( "Reading resourcePath {} ", resourcePath );

			URL resourceURL = null;

			try {
				// Our default functionality uses a preceding slash a-la loading a resource by name from the class. ClassLoader doesn't want the preceding slash.
				final String resourcePathForClassLoader = resourcePath.substring( 1 );

				// We're using this method to locate resources, in case there's more than one resource on the classpath with the same name
				final Enumeration<URL> resources = NGResourceLoader.class.getClassLoader().getResources( resourcePathForClassLoader );

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

			// If we didn't find the resource, warn the user
			if( resourceURL == null ) {
				// FIXME: This is probably not a WARN-sitation. We're polluting the logs // Hugi 2023-02-02
				logger.warn( "Unable to locate resource {}", resourcePath );
				return Optional.empty();
			}

			try {
				final InputStream resourceAsStream = resourceURL.openStream();

				// FIXME: I don't see this happening, but better check for it and warn about it. Find out of openStream() can actually return null // Hugi 2023-01-30
				if( resourceAsStream == null ) {
					logger.warn( "Received null input stream from {}", resourcePath );
					return Optional.empty();
				}

				return Optional.of( resourceAsStream );
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

	/**
	 * FIXME: Work in progress on providing a wrapper class for resources // Hugi
	 *
	 * new NGResource( resourceURL::openStream, "filename.png", "image/jpeg", 5456l );
	 */
	public record NGResource(
			Callable<InputStream> inputStreamSupplier,
			String filename,
			String mimeType,
			Long length ) {

		public byte[] bytes() {
			try {
				return inputStreamSupplier().call().readAllBytes();
			}
			catch( Exception e ) {
				throw new RuntimeException( e );
			}
		}
	}
}