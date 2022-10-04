package ng.appserver.privates;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for reading resources
 */

public class NGUtils {

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
		return webserverResourcesSource.bytesforResourceWithPath( resourcePath );
	}

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 */
	public static Optional<byte[]> readAppResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );
		return appResourcesSource.bytesforResourceWithPath( resourcePath );
	}

	public static Optional<byte[]> readComponentResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );
		return componentResourcesSource.bytesforResourceWithPath( resourcePath );
	}

	/**
	 * Represents a source of resources of any type
	 */
	public interface ResourceSource {
		public Optional<byte[]> bytesforResourceWithPath( String resourcePath );
	}

	/**
	 * Wraps loading of resources from the classpath
	 */
	public static class JavaClasspathResourceSource implements ResourceSource {

		private static final Logger logger = LoggerFactory.getLogger( JavaClasspathResourceSource.class );

		/**
		 * Classpath prefix
		 */
		private String _basePath;

		public JavaClasspathResourceSource( final String basePath ) {
			Objects.requireNonNull( basePath );
			_basePath = basePath;
		}

		@Override
		public Optional<byte[]> bytesforResourceWithPath( String resourcePath ) {
			Objects.requireNonNull( resourcePath );

			logger.info( "Reading resource {} ", resourcePath );

			resourcePath = pathWithPrefix( resourcePath );

			logger.info( "Reading resourcePath {} ", resourcePath );

			try( final InputStream resourceAsStream = NGUtils.class.getResourceAsStream( resourcePath )) {

				if( resourceAsStream == null ) {
					return Optional.empty();
				}

				return Optional.of( resourceAsStream.readAllBytes() );
			}
			catch( final IOException e ) {
				throw new RuntimeException( e );
			}
		}

		/**
		 * @return The path to the named resource
		 */
		private String pathWithPrefix( String resourcePath ) {
			return "/" + _basePath + "/" + resourcePath;
		}
	}
}