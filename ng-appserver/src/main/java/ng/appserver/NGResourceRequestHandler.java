package ng.appserver;

import java.util.Optional;

import ng.appserver.privates.NGMimeTypeDetector;

/**
 * FIXME: Ideally I'd like this to work with streams, not byte arrays. In that case it just becomes the responsibility of this code to link up the file/socket streams
 */

public class NGResourceRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( final NGRequest request ) {
		final Optional<String> resourcePath = Optional.of( resourcePathFromURI( request.uri() ) );

		if( resourcePath.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final Optional<byte[]> resourceBytes = NGApplication.application().resourceManager().bytesForWebserverResourceNamed( resourcePath.get() );

		// FIXME: How to handle this properly? User configurable? Just always a 404 // Hugi 2021-12-06
		if( resourceBytes.isEmpty() ) {
			final NGResponse errorResponse = new NGResponse( "webserver resources '" + resourcePath.get() + "' does not exist", 404 );
			errorResponse.setHeader( "content-type", "text/html" );
			return errorResponse;
		}

		// Extract the name of the served resource to use in the filename header
		final String resourceName = resourcePath.get().substring( resourcePath.get().lastIndexOf( "/" ) + 1 );
		final String mimeType = NGMimeTypeDetector.mimeTypeForResourceName( resourcePath.get() );

		// FIXME: Detect and set the correct response headers
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
		return uri.substring( "/wr/".length() );
	}
}