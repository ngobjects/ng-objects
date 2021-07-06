package ng.appserver.privates;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGResourceManager;

public class NGUtils {

	private static final Logger logger = LoggerFactory.getLogger( NGUtils.class );

	/**
	 * Reads the content of the given Java resource
	 */
	public static Optional<byte[]> readJavaResource( final String resourcePath ) {
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
		return "/" + folderName + "/" + resourcePath;
	}
}