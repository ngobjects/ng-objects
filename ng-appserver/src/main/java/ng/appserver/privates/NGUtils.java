package ng.appserver.privates;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGResourceManager;

/**
 * Utilities for reading resources
 */

public class NGUtils {

	private static final Logger logger = LoggerFactory.getLogger( NGUtils.class );

	/**
	 * Name of the folder that stores application resources
	 */
	private static final String APP_RESOURCES_FOLDER = "app-resources";

	/**
	 * Name of the folder that stores component templates
	 */
	private static final String COMPONENTS_FOLDER = "components";

	/**
	 * @return The named resource if it exists, an empty optional if not found
	 */
	public static Optional<byte[]> readAppResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );

		return readJavaResource( resourcePath( APP_RESOURCES_FOLDER, resourcePath ) );
	}

	public static Optional<byte[]> readComponentResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );

		return readJavaResource( resourcePath( COMPONENTS_FOLDER, resourcePath ) );
	}

	/**
	 * Reads the content of the given Java resource
	 */
	private static Optional<byte[]> readJavaResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );

		logger.info( "Reading resource from path: " + resourcePath );

		try( final InputStream resourceAsStream = NGResourceManager.class.getResourceAsStream( resourcePath )) {

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
	private static String resourcePath( final String folderName, final String resourcePath ) {
		Objects.requireNonNull( folderName );
		Objects.requireNonNull( resourcePath );

		return "/" + folderName + "/" + resourcePath;
	}
}