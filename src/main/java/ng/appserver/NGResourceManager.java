package ng.appserver;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.privates.NGUtils;

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

		return NGUtils.readJavaResource( resourceName );
	}
}