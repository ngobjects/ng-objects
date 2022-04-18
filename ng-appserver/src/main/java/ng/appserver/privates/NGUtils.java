package ng.appserver.privates;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGResourceManager;

public class NGUtils {

	private static final Logger logger = LoggerFactory.getLogger( NGUtils.class );

	private static final String APP_RESOURCES_FOLDER = "app-resources";
	private static final String COMPONENTS_FOLDER = "components";

	/**
	 * Reads the content of the given Java resource
	 */
	public static Optional<byte[]> readJavaResource( final String resourcePath ) {
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

	public static String resourcePath( final String folderName, final String resourcePath ) {
		Objects.requireNonNull( folderName );
		Objects.requireNonNull( resourcePath );

		return "/" + folderName + "/" + resourcePath;
	}

	public static Optional<byte[]> readAppResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );
		return readJavaResource( resourcePath( APP_RESOURCES_FOLDER, resourcePath ) );
	}

	public static Optional<byte[]> readComponentResource( final String resourcePath ) {
		Objects.requireNonNull( resourcePath );
		return readJavaResource( resourcePath( COMPONENTS_FOLDER, resourcePath ) );
	}
}