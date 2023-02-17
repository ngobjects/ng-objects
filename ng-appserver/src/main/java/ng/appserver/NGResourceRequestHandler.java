package ng.appserver;

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

		// FIXME: We want this to work with streams, not byte arrays. In that case it just becomes the responsibility of this code to link up the file/socket streams // Hugi 2023-02-17
		final Optional<byte[]> resourceBytes = NGApplication.application().resourceManager().bytesForWebserverResourceNamed( resourcePath );

		// FIXME: How to handle this properly? User configurable? Just always a 404? // Hugi 2021-12-06
		if( resourceBytes.isEmpty() ) {
			final NGResponse errorResponse = new NGResponse( "webserver resource '" + resourcePath + "' does not exist", 404 );
			errorResponse.setHeader( "content-type", "text/html" );
			return errorResponse;
		}

		// Extract the name of the served resource to use in the filename header
		final String resourceName = resourcePath.substring( resourcePath.lastIndexOf( "/" ) + 1 );
		final String mimeType = NGMimeTypeDetector.mimeTypeForResourceName( resourcePath );

		// FIXME: Detect and set the correct response headers, especially with regard to caching // Hugi 2023-02-17
		final NGResponse response = new NGResponse( resourceBytes.get(), 200 );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resourceName ) );
		response.setHeader( "Content-Type", mimeType );
		return response;
	}

	/**
	 * FIXME: Not at all a good method for getting the resource URI
	 *
	 * @return The resource path from the given URI
	 */
	private static String resourcePathFromURI( final String uri ) {
		return uri.substring( DEFAULT_PATH.length() );
	}
}