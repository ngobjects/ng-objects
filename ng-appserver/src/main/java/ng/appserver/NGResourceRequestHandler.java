package ng.appserver;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import ng.appserver.privates.NGMimeTypeDetector;

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
		final String resourcePath = resourcePathFromURI( request.uri() );

		if( resourcePath.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		// FIXME: We want this to work with streams, not byte arrays.
		// To make this work, we'll have to cache a wrapper class for the resource; that wrapper must give us a "stream provider", not an actual stream, since we'll be consuming the stream of a cached resource multiple times.
		// Hugi 2023-02-17
		final Optional<byte[]> resourceBytes = NGApplication.application().resourceManager().bytesForWebserverResourceNamed( resourcePath );

		return responseForResource( resourceBytes, resourcePath );
	}

	public static NGResponse responseForResource( Optional<byte[]> resourceBytes, final String resourcePath ) {

		// FIXME: Shouldn't we allow the user to customize the response for a non-existent resource? // Hugi 2021-12-06
		if( resourceBytes.isEmpty() ) {
			final NGResponse errorResponse = new NGResponse( "webserver resource '" + resourcePath + "' does not exist", 404 );
			errorResponse.setHeader( "content-type", "text/html" );
			return errorResponse;
		}

		// Extract the name of the served resource to use in the filename header
		final String resourceName = resourcePath.substring( resourcePath.lastIndexOf( "/" ) + 1 );
		final String mimeType = NGMimeTypeDetector.mimeTypeForResourceName( resourcePath );

		// FIXME: We need to allow some control over the headers for the returned resource, especially with regard to caching // Hugi 2023-02-17
		final NGResponse response = new NGResponse( resourceBytes.get(), 200 );
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
}