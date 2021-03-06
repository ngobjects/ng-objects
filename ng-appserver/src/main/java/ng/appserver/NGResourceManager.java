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
	private final Map<String, Optional<byte[]>> _webserverResourceCache = new ConcurrentHashMap<>();

	/**
	 * Specifies if we want to use the resources cache.
	 * FIXME: Current implementation is for testing only
	 */
	private boolean useCache() {
		return !NGApplication.application().properties().isDevelopmentMode();
	}

	public Optional<byte[]> bytesForWebserverResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		logger.debug( "Loading resource named {}. Caching: {}", resourceName, useCache() );

		Optional<byte[]> resource;

		if( useCache() ) {
			resource = _webserverResourceCache.get( resourceName );

			// FIXME: Applies to both non-existing and un-cached resources. Add an "I already checked this, it doesn't exist" resource cache entry
			if( resource == null ) {
				resource = NGUtils.readWebserverResource( resourceName );
				_webserverResourceCache.put( resourceName, resource );
			}
		}
		else {
			resource = NGUtils.readWebserverResource( resourceName );
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
	public Optional<String> urlForWebserverResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		final StringBuilder b = new StringBuilder();

		// FIXME: This is horrible just added for testing // Hugi
		//		if( !NGApplication.application().isDevelopmentMode() ) {
		//			b.append( "/Apps/WebObjects/Rebelliant.woa" );
		//		}

		b.append( "/wr/" );
		b.append( resourceName );

		return Optional.of( b.toString() );
	}

	/**
	 * @return The URL for the named resource
	 *
	 * FIXME: Whoa, that's incomplete
	 * FIXME: Determine if the resource exists first
	 * FIXME: I don't feel this belongs here. URL generation and resource management are separate things
	 */
	public Optional<String> urlForDynamicResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		final StringBuilder b = new StringBuilder();

		// FIXME: This is horriblem just added for testing // Hugi
		//		if( !NGApplication.application().isDevelopmentMode() ) {
		//			b.append( "/Apps/WebObjects/Rebelliant.woa" );
		//		}

		b.append( "/wd/" );
		b.append( resourceName );

		return Optional.of( b.toString() );
	}
}