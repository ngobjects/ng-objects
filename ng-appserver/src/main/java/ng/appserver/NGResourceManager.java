package ng.appserver;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.privates.NGUtils;

/**
 * Experimental implementation of the resource manager.
 * I'm not sure that this class should exist to begin with. It should be the responsibility of the bundle to locate resources.
 */

public class NGResourceManager {

	private static final Logger logger = LoggerFactory.getLogger( NGResourceManager.class );

	/**
	 * FIXME: Experimental cache
	 */
	private final Map<String, Optional<byte[]>> _cache = new ConcurrentHashMap<>();

	public Optional<byte[]> bytesForResourceNamed( final String resourceName ) {
		final String actualResourcePath = NGUtils.resourcePath( "app-resources", resourceName );

		logger.info( "Loading resource bytes {} from {}", resourceName, actualResourcePath );

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

	/**
	 * @return The URL for the named resource
	 *
	 * FIXME: Whoa, that's incomplete
	 * FIXME: Determine if the resource exists first
	 */
	public Optional<String> urlForResourceNamed( final String resourceName ) {
		return Optional.of( "/wr/" + resourceName );
	}

	private boolean useCache() {
		return false;
	}
}