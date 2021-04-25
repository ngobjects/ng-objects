package ng.appserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Experimental implementation of the resource manager
 */

public class NGResourceManager {

	private static final Logger logger = LoggerFactory.getLogger( NGResourceManager.class );

	/**
	 * FIXME: Return an Optional, return null or throw an exception on no resource?
	 */
	public Optional<byte[]> bytesForResourceWithName( final String resourceName ) {
		final String resourcePath = "/app-resources/" + resourceName;
		
		logger.info( "Loading resource {} at path {}", resourceName, resourcePath );

		try( final InputStream resourceAsStream = NGResourceManager.class.getResourceAsStream( resourcePath )) {

			if( resourceAsStream == null ) {
				return Optional.empty();
			}

			return Optional.of( resourceAsStream.readAllBytes() );
		}
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
	}
}