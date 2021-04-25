package ng.appserver;

import java.util.HashMap;
import java.util.Map;
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
	 * FIXME: Experimental cache
	 */
	private Map<String,Optional<byte[]>> _cache = new HashMap<>();

	public Optional<byte[]> bytesForResourceNamed( final String resourceName ) {
		final String actualResourcePath = "/app-resources/" + resourceName;

		logger.info( "Loading resource {} from {}", resourceName, actualResourcePath );

		if( useCache() ) {
			Optional<byte[]> resource = _cache.get( resourceName );
			
			if( resource == null ) {
				resource = NGUtils.readJavaResource( actualResourcePath );
				_cache.put( resourceName, resource );
			}
			
			return resource;
		}
		else {
			return NGUtils.readJavaResource( actualResourcePath );
		}
	}
	
	private boolean useCache() {
		return false;
	}
}