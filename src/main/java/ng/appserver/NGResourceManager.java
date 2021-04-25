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

	public Optional<byte[]> bytesForResourceNamed( final String resourceName ) {
		final String actualResourcePath = "/app-resources/" + resourceName;

		logger.info( "Loading resource {} from {}", resourceName, actualResourcePath );

		return NGUtils.readJavaResource( actualResourcePath );
	}
}