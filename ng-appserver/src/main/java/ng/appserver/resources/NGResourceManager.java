package ng.appserver.resources;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.resources.NGResourceLoader.JavaClasspathResourceSource;
import ng.appserver.resources.NGResourceLoader.StandardNamespace;

/**
 * Experimental implementation of the resource manager.
 * I'm not sure that this class should exist to begin with. It should be the responsibility of the bundle to locate resources.
 */

public class NGResourceManager {

	private static final Logger logger = LoggerFactory.getLogger( NGResourceManager.class );

	/**
	 * The resource loader that handles locating and loading of resources to be managed by this resource manager
	 */
	private NGResourceLoader _resourceLoader;

	/**
	 * Cache storing resources in-memory
	 */
	private final Map<ResourceType, Map<String, Optional<byte[]>>> resourceCache = new ConcurrentHashMap<>();

	/**
	 * Specifies if we want to use the resources cache.
	 */
	private static boolean _cachingEnabled() {
		return NGApplication.application().cachingEnabled();
	}

	/**
	 * @return The resource loader used by this manager
	 */
	private NGResourceLoader resourceLoader() {
		if( _resourceLoader == null ) {
			_resourceLoader = new NGResourceLoader();
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.App, new JavaClasspathResourceSource( "app-resources" ) );
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.WebServer, new JavaClasspathResourceSource( "webserver-resources" ) );
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.Public, new JavaClasspathResourceSource( "public" ) );
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.ComponentTemplate, new JavaClasspathResourceSource( "components" ) );
		}

		return _resourceLoader;
	}

	public Optional<byte[]> bytesForAppResourceNamed( final String resourceName ) {
		return bytesForAnyResource( resourceName, StandardResourceType.App );
	}

	public Optional<byte[]> bytesForWebserverResourceNamed( final String resourceName ) {
		return bytesForAnyResource( resourceName, StandardResourceType.WebServer );
	}

	public Optional<byte[]> bytesForComponentResourceNamed( final String resourceName ) {
		return bytesForAnyResource( resourceName, StandardResourceType.ComponentTemplate );
	}

	public Optional<byte[]> bytesForPublicResourceNamed( final String resourceName ) {
		return bytesForAnyResource( resourceName, StandardResourceType.Public );
	}

	private Optional<byte[]> bytesForAnyResource( final String resourceName, ResourceType resourceType ) {
		Objects.requireNonNull( resourceName );
		Objects.requireNonNull( resourceType );

		logger.debug( "Loading resource named {}. Caching: {}", resourceName, _cachingEnabled() );

		Optional<byte[]> resource;

		if( _cachingEnabled() ) {
			final Map<String, Optional<byte[]>> cacheMap = resourceCache.computeIfAbsent( resourceType, _unused -> new ConcurrentHashMap<>() );

			resource = cacheMap.get( resourceName );

			// FIXME: Applies to both non-existing and un-cached resources. Add an "I already checked this, it doesn't exist" resource cache entry
			if( resource == null ) {
				resource = resourceLoader().bytesForResource( resourceType, resourceName );
				cacheMap.put( resourceName, resource );
			}
		}
		else {
			resource = resourceLoader().bytesForResource( resourceType, resourceName );
		}

		return resource;
	}
}