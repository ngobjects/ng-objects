package ng.appserver;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.privates.NGResourceLoader;

/**
 * Experimental implementation of the resource manager.
 * I'm not sure that this class should exist to begin with. It should be the responsibility of the bundle to locate resources.
 */

public class NGResourceManager {

	private static final Logger logger = LoggerFactory.getLogger( NGResourceManager.class );

	// FIXME: The eventual type of cache we're going to have? (including namespaces though) // Hugi 2024-02-24
	// private final Map<ResourceType, Map<String, Optional<byte[]>>> resourceCache = new ConcurrentHashMap<>();

	/**
	 * FIXME: Experimental caches. Resource caches should be located centrally.
	 */
	private final Map<String, Optional<byte[]>> _appResourceCache = new ConcurrentHashMap<>();
	private final Map<String, Optional<byte[]>> _webserverResourceCache = new ConcurrentHashMap<>();
	private final Map<String, Optional<byte[]>> _componentResourceCache = new ConcurrentHashMap<>();
	private final Map<String, Optional<byte[]>> _publicResourceCache = new ConcurrentHashMap<>();

	/**
	 * Specifies if we want to use the resources cache.
	 */
	private static boolean _cachingEnabled() {
		return NGApplication.application().cachingEnabled();
	}

	/**
	 * @return The resource loader used by this manager
	 *
	 * FIXME: We're currently using a global instance, will eventually be replaced by a local instance // Hugi 2024-05-25
	 */
	private NGResourceLoader loader() {
		return NGResourceLoader.instance();
	}

	public Optional<byte[]> bytesForAppResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );
		return bytesForAnyResource( resourceName, _appResourceCache, loader()::bytesForAppResource );
	}

	public Optional<byte[]> bytesForWebserverResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );
		return bytesForAnyResource( resourceName, _webserverResourceCache, loader()::bytesForWebserverResource );
	}

	public Optional<byte[]> bytesForComponentResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );
		return bytesForAnyResource( resourceName, _componentResourceCache, loader()::bytesForComponentResource );
	}

	public Optional<byte[]> bytesForPublicResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );
		return bytesForAnyResource( resourceName, _publicResourceCache, loader()::bytesForPublicResource );
	}

	private static Optional<byte[]> bytesForAnyResource( final String resourceName, final Map<String, Optional<byte[]>> cacheMap, Function<String, Optional<byte[]>> readFunction ) {
		Objects.requireNonNull( resourceName );

		logger.debug( "Loading resource named {}. Caching: {}", resourceName, _cachingEnabled() );

		Optional<byte[]> resource;

		if( _cachingEnabled() ) {
			resource = cacheMap.get( resourceName );

			// FIXME: Applies to both non-existing and un-cached resources. Add an "I already checked this, it doesn't exist" resource cache entry
			if( resource == null ) {
				resource = readFunction.apply( resourceName );
				cacheMap.put( resourceName, resource );
			}
		}
		else {
			resource = readFunction.apply( resourceName );
		}

		return resource;
	}

	/**
	 * @return The URL for the named resource
	 *
	 * FIXME: Shouldn't be static
	 * FIXME: I don't feel this belongs here, URL generation will be dependent on the environment
	 */
	public static Optional<String> urlForDynamicResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		final StringBuilder b = new StringBuilder();
		b.append( "/wd/" );
		b.append( resourceName );

		return Optional.of( b.toString() );
	}
}