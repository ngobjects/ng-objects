package ng.appserver;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Experimental implementation of the resource manager
 */

public class NGResourceManager {

	private static final Logger logger = LoggerFactory.getLogger( NGResourceManager.class );

	public byte[] bytesForResourceWithName( final String resourceName ) {
		final String resourcePath = "/app-resources/" + resourceName;
		
		logger.info( "Loading resource {} at path {}", resourceName, resourcePath );

		try( final InputStream resourceAsStream = NGResourceManager.class.getResourceAsStream( resourcePath )) {
			return resourceAsStream.readAllBytes();
		}
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
	}
}