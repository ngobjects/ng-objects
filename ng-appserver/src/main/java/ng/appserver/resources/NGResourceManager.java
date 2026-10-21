package ng.appserver.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.resources.NGResourceStamps;

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
	 * The content stamp of each resource by type, namespace and path ({@code WebServer:app/css/site.css}), empty for one
	 * that doesn't exist. Only used when caching is enabled: resources don't change while a deployed application runs.
	 */
	private final Map<String, String> _stamps = new ConcurrentHashMap<>();

	/**
	 * Specifies if we want to use the resources cache.
	 */
	protected boolean cachingEnabled() {
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
	 * @return The content stamp of the specified webserver resource (see {@link NGResourceStamps}), null if it doesn't exist
	 */
	public String webserverResourceStamp( final String namespace, final String resourcePath ) {
		return stamp( namespace, StandardResourceType.WebServer, resourcePath );
	}

	/**
	 * @return The content stamp of the specified public resource (see {@link NGResourceStamps}), null if it doesn't exist
	 */
	public String publicResourceStamp( final String namespace, final String resourcePath ) {
		return stamp( namespace, StandardResourceType.Public, resourcePath );
	}

	/**
	 * @return The content stamp of the specified resource, null if it doesn't exist. Remembered when caching is enabled,
	 *         computed each time otherwise, as files change while a development application runs.
	 */
	private String stamp( final String namespace, final ResourceType resourceType, final String resourcePath ) {
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( resourceType );
		Objects.requireNonNull( resourcePath );

		// The same resource, with or without a preceding slash, as when its URL is generated
		final String path = resourcePath.startsWith( "/" ) ? resourcePath.substring( 1 ) : resourcePath;

		if( !cachingEnabled() ) {
			return computeStamp( namespace, resourceType, path );
		}

		final String stamp = _stamps.computeIfAbsent( resourceType + ":" + namespace + "/" + path, _unused -> {
			final String computed = computeStamp( namespace, resourceType, path );
			return computed == null ? "" : computed;
		} );

		return stamp.isEmpty() ? null : stamp;
	}

	/**
	 * @return The content stamp of the specified resource, read through it; null if it doesn't exist
	 */
	private String computeStamp( final String namespace, final ResourceType resourceType, final String resourcePath ) {
		final Optional<NGResource> resource = obtainResource( namespace, resourceType, resourcePath );

		if( resource.isEmpty() ) {
			return null;
		}

		try( final InputStream content = resource.get().inputStream() ) {
			return NGResourceStamps.stamp( content );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
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

		logger.debug( "Loading {} resource {}::{}. Caching: {}", resourceType, namespace, resourcePath, cachingEnabled() );

		if( cachingEnabled() ) {
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