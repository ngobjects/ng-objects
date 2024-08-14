package ng.appserver.resources;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;

/**
 * Manages static resources and resource caching
 */

public class NGResourceManager {

	private static final Logger logger = LoggerFactory.getLogger( NGResourceManager.class );

	/**
	 * The resource loader that handles locating and loading of resources to be managed by this resource manager
	 */
	private NGResourceLoader _resourceLoader = new NGResourceLoader();

	/**
	 * Cache storing resources in-memory by namespace -> resource type -> resource path
	 */
	private final Map<String, Map<ResourceType, Map<String, Optional<NGResource>>>> resourceCache = new ConcurrentHashMap<>();

	/**
	 * Specifies if we want to use the resources cache.
	 */
	private static boolean _cachingEnabled() {
		return NGApplication.application().cachingEnabled();
	}

	/**
	 * @return The resource loader used by this manager
	 */
	public NGResourceLoader resourceLoader() {
		return _resourceLoader;
	}

	/**
	 * @return The specified app resource
	 */
	public Optional<NGResource> obtainAppResource( final String namespace, final String resourcePath ) {
		return obtainResource( namespace, StandardResourceType.App, resourcePath );
	}

	/**
	 * @return The specified webserver resource
	 */
	public Optional<NGResource> obtainWebserverResource( final String namespace, final String resourcePath ) {
		return obtainResource( namespace, StandardResourceType.WebServer, resourcePath );
	}

	/**
	 * @return The specified component template resource
	 */
	public Optional<NGResource> obtainComponentTemplateResource( final String namespace, final String resourcePath ) {
		return obtainResource( namespace, StandardResourceType.ComponentTemplate, resourcePath );
	}

	/**
	 * @return The specified public resource
	 */
	public Optional<NGResource> obtainPublicResource( final String namespace, final String resourcePath ) {
		return obtainResource( namespace, StandardResourceType.Public, resourcePath );
	}

	/**
	 * @return The bytes for the named resource, looking for a cached copy first if caching is enabled (i.e. if we're in production mode)
	 */
	private Optional<NGResource> obtainResource( final String namespace, final ResourceType resourceType, final String resourcePath ) {
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( resourcePath );
		Objects.requireNonNull( resourceType );

		logger.debug( "Loading {} resource {}::{}. Caching: {}", resourceType, namespace, resourcePath, _cachingEnabled() );

		if( _cachingEnabled() ) {
			return resourceCache
					.computeIfAbsent( namespace, _unused -> new ConcurrentHashMap<>() )
					.computeIfAbsent( resourceType, _unused -> new ConcurrentHashMap<>() )
					.computeIfAbsent( resourcePath, _unused -> resourceLoader().obtainResource( namespace, resourceType, resourcePath ) );
		}

		return resourceLoader().obtainResource( namespace, resourceType, resourcePath );
	}

	/**
	 * @return the specified resource by searching all namespaces
	 */
	@Deprecated
	private Optional<NGResource> obtainResourceSearchingAllNamespaces( final ResourceType resourceType, final String resourcePath ) {
		for( String namespace : resourceLoader().namespaces() ) {
			final Optional<NGResource> resource = obtainResource( namespace, resourceType, resourcePath );

			if( !resource.isEmpty() ) {
				return resource;
			}
		}

		return Optional.empty();
	}

	/**
	 * @return The specified component template resource by searching in all namespaces
	 */
	@Deprecated
	public Optional<NGResource> obtainComponentTemplateResourceSearchingAllNamespaces( final String resourcePath ) {
		return obtainResourceSearchingAllNamespaces( StandardResourceType.ComponentTemplate, resourcePath );
	}
}