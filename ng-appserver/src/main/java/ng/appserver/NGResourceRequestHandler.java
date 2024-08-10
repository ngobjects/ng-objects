package ng.appserver;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.resources.NGMimeTypes;
import ng.appserver.resources.NGResource;

/**
 * Request handler for serving webserver-resources
 *
 * FIXME: Missing namespace handling // Hugi 2024-06-17
 */

public class NGResourceRequestHandler extends NGRequestHandler {

	/**
	 * The default path prefix for this request handler
	 */
	public static final String DEFAULT_PATH = "/wr/";

	@Override
	public NGResponse handleRequest( final NGRequest request ) {

		// FIXME: We're still missing the actual namespace, so we just hardcode the app namespace for now // Hugi 2024-08-10
		final String namespace = "app";
		final String resourcePath = resourcePathFromURI( request.uri() );

		if( resourcePath.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final Optional<NGResource> resource = NGApplication.application().resourceManager().obtainWebserverResource( namespace, resourcePath );

		// FIXME: We need to allow the user to customize the response for a non-existent resource // Hugi 2021-12-06
		if( resource.isEmpty() ) {
			final NGResponse errorResponse = new NGResponse( "webserver resource '%s' does not exist".formatted( resourcePath ), 404 );
			errorResponse.setHeader( "content-type", "text/html" );
			return errorResponse;
		}

		return responseForResource( resource.get(), resourcePath );
	}

	@Deprecated
	public static NGResponse responseForResource( final NGResource resource, final String resourcePath ) {

		// Extract the name of the served resource to use in the filename header
		final String resourceName = resourcePath.substring( resourcePath.lastIndexOf( "/" ) + 1 );
		final String mimeType = NGMimeTypes.mimeTypeForResourceName( resourcePath );

		// FIXME: We need to allow some control over the headers for the returned resource, especially with regard to caching // Hugi 2023-02-17
		final NGResponse response = new NGResponse( resource.bytes(), 200 );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resourceName ) );
		response.setHeader( "Content-Type", mimeType );
		return response;
	}

	/**
	 * @return The resource path from the given URI
	 */
	private static String resourcePathFromURI( final String uri ) {
		String pathString = uri.substring( DEFAULT_PATH.length() );

		// FIXME: We might want to look a little into this URL decoding, both the the applied charset and the location of the logic // Hugi 2024-05-24
		// This is added purely as a fix for resource loading, but URL decoding might actually (probably) have to happen at the base request handling level
		pathString = URLDecoder.decode( pathString, StandardCharsets.UTF_8 );

		return pathString;
	}

	/**
	 * @return The URL for the named resource
	 *
	 * FIXME: Shouldn't be static
	 * FIXME: Determine if the resource exists before generating URLs
	 * FIXME: I don't feel this belongs here, URL generation will be dependent on the environment
	 * FIXME: Missing namespace handling // Hugi 2024-06-17
	 */
	public static Optional<String> urlForWebserverResourceNamed( String namespace, String resourcePath ) {
		Objects.requireNonNull( resourcePath );

		// Since we don't use the concept of "relative paths", we can always assume an absolute path
		// (meaning we can remove preceding slashes and always navigate from root)
		// FIXME: While allowing paths with and without preceding slashes may be nice, it may be *nicer* to standardize a practice of either-or // Hugi 2024-05-25
		if( resourcePath.startsWith( "/" ) ) {
			resourcePath = resourcePath.substring( 1 );
		}

		final StringBuilder b = new StringBuilder();
		b.append( DEFAULT_PATH );
		b.append( resourcePath );

		return Optional.of( b.toString() );
	}
}