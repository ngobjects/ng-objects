package ng.appserver.resources;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.resources.NGResourceLoader.JavaClasspathResourceSource;

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
	 * Cache storing resources in-memory by namespace -> resource type -> resource path
	 */
	private final Map<String, Map<ResourceType, Map<String, Optional<byte[]>>>> resourceCache = new ConcurrentHashMap<>();

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

			// FIXME: These are the "unnamespaced" resource locations we started ou with. They'll still work fine, but we'll need to consider their future // Hugi 2024-06-19
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.App, new JavaClasspathResourceSource( "app-resources" ) );
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.WebServer, new JavaClasspathResourceSource( "webserver-resources" ) );
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.Public, new JavaClasspathResourceSource( "public" ) );
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.ComponentTemplate, new JavaClasspathResourceSource( "components" ) );

			// "app" namespace defined
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.App, new JavaClasspathResourceSource( "ng/app/app-resources" ) );
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.WebServer, new JavaClasspathResourceSource( "ng/app/webserver-resources" ) );
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.Public, new JavaClasspathResourceSource( "ng/app/public" ) );
			_resourceLoader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.ComponentTemplate, new JavaClasspathResourceSource( "ng/app/components" ) );
		}

		return _resourceLoader;
	}

	/**
	 * @return The specified app resource
	 */
	public Optional<byte[]> bytesForAppResourceNamed( final String namespace, final String resourcePath ) {
		return bytesForResource( namespace, StandardResourceType.App, resourcePath );
	}

	/**
	 * @return The specified app resource by searching in all namespaces
	 */
	@Deprecated
	public Optional<byte[]> bytesForAppResourceNamed( final String resourcePath ) {
		return bytesForResourceSearchingAllNamespaces( StandardResourceType.App, resourcePath );
	}

	/**
	 * @return The specified webserver resource
	 */
	public Optional<byte[]> bytesForWebserverResourceNamed( final String namespace, final String resourcePath ) {
		return bytesForResource( namespace, StandardResourceType.WebServer, resourcePath );
	}

	/**
	 * @return The specified webserver resource by searching in all namespaces
	 */
	@Deprecated
	public Optional<byte[]> bytesForWebserverResourceNamed( final String resourcePath ) {
		return bytesForResourceSearchingAllNamespaces( StandardResourceType.WebServer, resourcePath );
	}

	/**
	 * @return The specified component template resource
	 */
	public Optional<byte[]> bytesForComponentTemplateResourceNamed( final String namespace, final String resourcePath ) {
		return bytesForResource( namespace, StandardResourceType.ComponentTemplate, resourcePath );
	}

	/**
	 * @return The specified component template resource by searching in all namespaces
	 */
	@Deprecated
	public Optional<byte[]> bytesForComponentTemplateResourceNamed( final String resourcePath ) {
		return bytesForResourceSearchingAllNamespaces( StandardResourceType.ComponentTemplate, resourcePath );
	}

	/**
	 * @return The specified public resource
	 */
	public Optional<byte[]> bytesForPublicResourceNamed( final String namespace, final String resourcePath ) {
		return bytesForResource( namespace, StandardResourceType.Public, resourcePath );
	}

	/**
	 * @return The specified public resource resource by searching in all namespaces
	 */
	@Deprecated
	public Optional<byte[]> bytesForPublicResourceNamed( final String resourcePath ) {
		return bytesForResourceSearchingAllNamespaces( StandardResourceType.Public, resourcePath );
	}

	/**
	 * @return The bytes for the named resource, looking for a cached copy first if caching is enabled (i.e. if we're in production mode)
	 */
	private Optional<byte[]> bytesForResource( final String namespace, final ResourceType resourceType, final String resourcePath ) {
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( resourcePath );
		Objects.requireNonNull( resourceType );

		logger.debug( "Loading {} resource {}::{}. Caching: {}", resourceType, namespace, resourcePath, _cachingEnabled() );

		if( _cachingEnabled() ) {
			return resourceCache
					.computeIfAbsent( namespace, _unused -> new ConcurrentHashMap<>() )
					.computeIfAbsent( resourceType, _unused -> new ConcurrentHashMap<>() )
					.computeIfAbsent( resourcePath, _unused -> resourceLoader().bytesForResource( namespace, resourceType, resourcePath ) );
		}

		return resourceLoader().bytesForResource( namespace, resourceType, resourcePath );
	}

	/**
	 * @return bytes for the specified resource by searching all namespaces
	 */
	@Deprecated
	private Optional<byte[]> bytesForResourceSearchingAllNamespaces( final ResourceType resourceType, final String resourcePath ) {
		for( String namespace : resourceLoader().namespaces() ) {
			final Optional<byte[]> resourceBytes = bytesForResource( namespace, resourceType, resourcePath );

			if( !resourceBytes.isEmpty() ) {
				return resourceBytes;
			}
		}

		return Optional.empty();
	}
}