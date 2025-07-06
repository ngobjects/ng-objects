package ng.appserver;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.resources.NGMimeTypes;
import ng.appserver.resources.NGResource;

/**
 * Request handler for serving webserver-resources
 */

public class NGResourceRequestHandler extends NGRequestHandler {

	/**
	 * The default path prefix for this request handler
	 */
	public static final String DEFAULT_PATH = "/wr/";

	@Override
	public NGResponse handleRequest( final NGRequest request ) {

		final String url = request.uri();

		// FIXME: Some horrid URL parsing. We need to get choppin' on making NGParsedURI more usable for parsing like this // Hugi 2024-10-12
		int firstSlashIndex = DEFAULT_PATH.length();
		int secondSlashIndex = url.indexOf( '/', firstSlashIndex );

		String namespace = url.substring( firstSlashIndex, secondSlashIndex );

		if( namespace.isEmpty() ) {
			return new NGResponse( "No resource namespace specified", 400 );
		}

		String resourcePath = url.substring( secondSlashIndex + 1 );

		if( resourcePath.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		// FIXME:
		// Look into and standardize this decoding later, both the the applied charset and the location of the logic.
		// Added as a fix for resource loading, but URL decoding probably needs to happen at the base request handling level
		// Hugi 2024-05-24
		namespace = URLDecoder.decode( namespace, StandardCharsets.UTF_8 );
		resourcePath = URLDecoder.decode( resourcePath, StandardCharsets.UTF_8 );

		final Optional<NGResource> resource = NGApplication.application().resourceManager().obtainWebserverResource( namespace, resourcePath );

		if( resource.isEmpty() ) {
			return responseForNonExistentResource( namespace, resourcePath );
		}

		return responseForResource( resource.get(), resourcePath );
	}

	/**
	 * @return The response served when an identified resource doesn't exist
	 *
	 * FIXME: Shouldn't be static and doesn't belong here // Hugi 2024-10-12
	 * FIXME: Allow the user to customize the response for a non-existent resource // Hugi 2024-10-11
	 */
	private static NGResponse responseForNonExistentResource( final String namespace, final String resourcePath ) {
		final NGResponse errorResponse = new NGResponse( "webserver resource '%s':'%s' does not exist".formatted( namespace, resourcePath ), 404 );
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
		final NGResponse response = new NGResponse( resource.bytes(), 200 );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resourceName ) );
		response.setHeader( "Content-Type", mimeType );

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

		final StringBuilder b = new StringBuilder();
		b.append( DEFAULT_PATH );
		b.append( namespace );
		b.append( '/' );
		b.append( resourcePath );
		return Optional.of( b.toString() );
	}
}