package ng.appserver;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.http.NGRequest;
import ng.appserver.http.NGResponses;
import ng.appserver.http.NGResponse;
import ng.appserver.resources.NGMimeTypes;
import ng.appserver.resources.NGResource;
import ng.appserver.resources.NGResourceManager;
import ng.resources.NGResourceStamps;

/**
 * Request handler for serving webserver-resources
 */

public class NGResourceRequestHandler extends NGRequestHandler {

	/**
	 * The default path prefix for this request handler
	 */
	public static final String DEFAULT_PATH = "/nr/";

	@Override
	public NGResponse handleRequest( final NGRequest request ) {

		final String url = request.uri();

		// FIXME: Some horrid URL parsing. We need to get choppin' on making NGParsedURI more usable for parsing like this // Hugi 2024-10-12
		int firstSlashIndex = DEFAULT_PATH.length();
		int secondSlashIndex = url.indexOf( '/', firstSlashIndex );

		String namespace = url.substring( firstSlashIndex, secondSlashIndex );

		if( namespace.isEmpty() ) {
			return NGResponses.of( 400, "No resource namespace specified" );
		}

		String resourcePath = url.substring( secondSlashIndex + 1 );

		if( resourcePath.isEmpty() ) {
			return NGResponses.of( 400, "No resource name specified" );
		}

		// FIXME:
		// Look into and standardize this decoding later, both the the applied charset and the location of the logic.
		// Added as a fix for resource loading, but URL decoding probably needs to happen at the base request handling level
		// Hugi 2024-05-24
		namespace = URLDecoder.decode( namespace, StandardCharsets.UTF_8 );
		resourcePath = URLDecoder.decode( resourcePath, StandardCharsets.UTF_8 );

		final NGResourceManager resourceManager = NGApplication.application().resourceManager();

		// A stamped name (css/site@3f9c1e07ab.css, as generated URLs carry it) names the resource without the stamp.
		// If there's nothing by that name, the name may be a file's own, and is looked up as it is.
		final NGResourceStamps.Stamped stamped = NGResourceStamps.parse( resourcePath );
		String servedPath = stamped != null ? stamped.unstampedPath() : resourcePath;
		String requestedStamp = stamped != null ? stamped.stamp() : null;
		Optional<NGResource> resource = resourceManager.obtainWebserverResource( namespace, servedPath );

		if( resource.isEmpty() && stamped != null ) {
			servedPath = resourcePath;
			requestedStamp = null;
			resource = resourceManager.obtainWebserverResource( namespace, servedPath );
		}

		if( resource.isEmpty() ) {
			return responseForNonExistentResource( namespace, resourcePath );
		}

		final String currentStamp = resourceManager.webserverResourceStamp( namespace, servedPath );
		return conditionalResponseForResource( resource.get(), servedPath, requestedStamp, currentStamp, request.headerForKey( "if-none-match" ), NGApplication.application().cachingEnabled() );
	}

	/**
	 * @param requestedStamp The stamp the requested URL carried, null if none
	 * @param currentStamp The stamp of the resource's current content, null if it has none
	 * @param ifNoneMatch The request's If-None-Match header, null if none
	 * @param cachingEnabled true if resources don't change while the application runs (in production)
	 *
	 * @return A response for the given resource. The resource's stamp is its ETag, and a request naming it gets a 304.
	 *         Requested by its current stamp, a resource is cached for good, as the URL changes whenever the content does.
	 *         Otherwise the browser may keep it, but must ask again before using it: an unstamped URL (a public resource's
	 *         always is), or a stamp that isn't the resource's own, as when an instance that hasn't been updated yet gets a
	 *         request for a newer version during a deploy.
	 */
	static NGResponse conditionalResponseForResource( final NGResource resource, final String resourcePath, final String requestedStamp, final String currentStamp, final String ifNoneMatch, final boolean cachingEnabled ) {
		final String etag = currentStamp == null ? null : "\"" + currentStamp + "\"";
		final boolean current = cachingEnabled && requestedStamp != null && requestedStamp.equals( currentStamp );
		final String cacheControl = current ? STAMPED_CACHE_CONTROL : "no-cache";

		if( etag != null && matchesAny( ifNoneMatch, etag ) ) {
			final NGResponse notModified = NGResponses.of();
			notModified.setStatus( 304 );
			notModified.setHeader( "etag", etag );
			notModified.setHeader( "cache-control", cacheControl );
			return notModified;
		}

		final NGResponse response = responseForResource( resource, resourcePath );
		response.setHeader( "cache-control", cacheControl );

		if( etag != null ) {
			response.setHeader( "etag", etag );
		}

		return response;
	}

	/**
	 * @return true if an If-None-Match header names the given entity tag, or is {@code *}. Compared weakly, as the header's
	 *         semantics require: a {@code W/} prefix doesn't matter.
	 */
	static boolean matchesAny( final String ifNoneMatch, final String etag ) {

		if( ifNoneMatch == null ) {
			return false;
		}

		for( String candidate : ifNoneMatch.split( "," ) ) {
			candidate = candidate.trim();

			if( candidate.startsWith( "W/" ) ) {
				candidate = candidate.substring( 2 );
			}

			if( candidate.equals( "*" ) || candidate.equals( etag ) ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * The cache lifetime of a resource requested by a URL with its current stamp: a year, and never revalidated, as the URL
	 * changes whenever the content does
	 */
	private static final String STAMPED_CACHE_CONTROL = "public, max-age=31536000, immutable";

	/**
	 * @return The response served when an identified resource doesn't exist
	 *
	 * FIXME: Shouldn't be static and doesn't belong here // Hugi 2024-10-12
	 * FIXME: Allow the user to customize the response for a non-existent resource // Hugi 2024-10-11
	 */
	private static NGResponse responseForNonExistentResource( final String namespace, final String resourcePath ) {
		final NGResponse errorResponse = NGResponses.of( 404, "webserver resource '%s':'%s' does not exist".formatted( namespace, resourcePath ) );
		errorResponse.setHeader( "content-type", "text/html" );
		return errorResponse;
	}

	/**
	 * @return Response for serving the given resource
	 *
	 * FIXME: Shouldn't be static and doesn't belong here // Hugi 2024-10-12
	 */
	@Deprecated
	public static NGResponse responseForResource( final NGResource resource, final String resourcePath ) {

		// Extract the name of the served resource to use in the filename header
		final String resourceName = resourcePath.substring( resourcePath.lastIndexOf( "/" ) + 1 );
		final String mimeType = NGMimeTypes.mimeTypeForResourceName( resourcePath );

		// FIXME: We need to allow some control over the headers for the returned resource // Hugi 2023-02-17
		final NGResponse response = NGResponses.ok( resource.bytes() );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resourceName ) );
		response.setHeader( "Content-Type", mimeType ); // FIXME: For some text resources the framework must be able to include a charset for the returned content-type (like text/css; charset=utf-8) // Hugi 2026-05-26

		// FIXME: Allowing header control for resource headers becomes especially important WRT caching. We might want to consider encapsulating construction of the cache-control header in an API // Hugi 2025-07-06
		response.setHeader( "cache-control", "max-age=3600" );

		return response;
	}

	/**
	 * @return The URL for the named resource
	 *
	 * FIXME: Shouldn't be static and doesn't belong here. Should end up as a part of a forthcoming standard route URL generation mechanism // Hugi 2024-10-12
	 * FIXME: Determine if the resource exists before generating URLs // Hugi 2024-10-12
	 */
	public static Optional<String> urlForWebserverResourceNamed( String namespace, String resourcePath ) {
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( resourcePath );

		// Since we don't use the concept of "relative paths", we can always assume an absolute path (meaning we can remove preceding slashes and always navigate from root)
		// FIXME: While allowing paths with and without preceding slashes may be nice, it may be *nicer* to standardize a practice of either-or // Hugi 2024-05-25
		if( resourcePath.startsWith( "/" ) ) {
			resourcePath = resourcePath.substring( 1 );
		}

		// In production, the resource name carries a stamp of the resource's content (css/site@3f9c1e07ab.css), so the URL
		// changes whenever the content does and browsers can cache it for good. Not in development, where files change while the application runs.
		final NGApplication application = NGApplication.application();

		if( application != null && application.cachingEnabled() ) {
			final String stamp = application.resourceManager().webserverResourceStamp( namespace, resourcePath );

			if( stamp != null ) {
				resourcePath = NGResourceStamps.stampedPath( resourcePath, stamp );
			}
		}

		final StringBuilder b = new StringBuilder();
		b.append( DEFAULT_PATH );
		b.append( namespace );
		b.append( '/' );
		b.append( resourcePath );
		return Optional.of( b.toString() );
	}
}