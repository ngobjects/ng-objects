package ng.appserver;

import java.util.Map;
import java.util.Objects;
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
	 * FIXME: Resource caches should be located centrally
	 */
	private final Map<String, Optional<byte[]>> _resourceCache = new ConcurrentHashMap<>();

	/**
	 * Specifies if we want to use the resources cache.
	 * FIXME: Current implementation is for testing only
	 */
	private boolean useCache() {
		return !NGApplication.application().properties().isDevelopmentMode();
	}

	public Optional<byte[]> bytesForResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		final String actualResourcePath = NGUtils.resourcePath( "app-resources", resourceName );

		logger.info( "Loading resource {} from {}. Caching: {}", resourceName, actualResourcePath, useCache() );

		Optional<byte[]> resource;

		if( useCache() ) {
			resource = _resourceCache.get( resourceName );

			// FIXME: Applies to both non-existing and un-cached resources. Add an "I already checked this, it doesn't exist" resource cache entry
			if( resource == null ) {
				resource = NGUtils.readJavaResource( actualResourcePath );
				_resourceCache.put( resourceName, resource );
			}
		}
		else {
			resource = NGUtils.readJavaResource( actualResourcePath );
		}

		return resource;
	}

	/**
	 * @return The URL for the named resource
	 *
	 * FIXME: Whoa, that's incomplete
	 * FIXME: Determine if the resource exists first
	 * FIXME: I don't feel this belongs here. URL generation and resource management are separate things
	 */
	public Optional<String> urlForResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		return Optional.of( "/Apps/WebObjects/Rebelliant.woa/wr/" + resourceName );
	}
}