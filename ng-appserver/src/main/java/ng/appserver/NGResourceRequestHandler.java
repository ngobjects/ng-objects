package ng.appserver;

import java.util.Optional;

import ng.appserver.privates.NGMimeTypeDetector;
import ng.appserver.privates.NGParsedURI;

/**
 * FIXME: Ideally I'd like this to work with streams, not byte arrays. In that case it just becomes the responsibility of this code to link up the file/socket streams
 */

public class NGResourceRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( final NGRequest request ) {
		final Optional<String> resourceName = NGParsedURI.of( request.uri() ).getStringOptional( 1 );

		if( resourceName.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final Optional<byte[]> resourceBytes = NGApplication.application().resourceManager().bytesForWebserverResourceNamed( resourceName.get() );

		// FIXME: How to handle this properly? User configurable? Just always a 404 // Hugi 2021-12-06
		if( resourceBytes.isEmpty() ) {
			final NGResponse errorResponse = new NGResponse( "webserver resources '" + resourceName.get() + "' does not exist", 404 );
			errorResponse.setHeader( "content-type", "text/html" );
			return errorResponse;
		}

		final String mimeType = NGMimeTypeDetector.mimeTypeForResourceName( resourceName.get() );

		// FIXME: Detect and set the correct response headers
		final NGResponse response = new NGResponse( resourceBytes.get(), 200 );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resourceName.get() ) );
		response.setHeader( "Content-Type", mimeType );
		return response;
	}
}