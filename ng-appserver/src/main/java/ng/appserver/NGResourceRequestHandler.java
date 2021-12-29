package ng.appserver;

import java.util.Objects;
import java.util.Optional;

import ng.appserver.privates.NGParsedURI;

/**
 * FIXME: Ideally I'd like this to work with streams, not byte arrays. In that case it just becomes the responsibility of this code to link up the file/socket streams
 */

public class NGResourceRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( final NGRequest request ) {
		final Optional<String> resourceName = NGParsedURI.of( request.uri() ).elementAt( 1 );

		if( resourceName.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final Optional<byte[]> resourceBytes = NGApplication.application().resourceManager().bytesForResourceNamed( resourceName.get() );

		// FIXME: How to handle this properly? User configurable? Just always a 404
		if( resourceBytes.isEmpty() ) {
			return new NGResponse( "Resource '" + resourceName.get() + "' does not exist", 404 );
		}

		final String mimeType = mimeTypeForResourceName( resourceName.get() );

		// FIXME: Detect and set the correct response headers
		final NGResponse response = new NGResponse( resourceBytes.get(), 200 );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resourceName.get() ) );
		response.setHeader( "Content-Type", mimeType );
		return response;
	}

	/**
	 * Highly advanced mime type detection
	 *
	 * FIXME: Do diz // Hugi 2021-12-29
	 */
	private static final String mimeTypeForResourceName( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		if( resourceName.endsWith( ".jpg" ) ) {
			return "image/jpeg";
		}

		throw new IllegalArgumentException( "For some reason, our advanced algorithm has not managed to identify your resource type. Is it possible it's not a JPEG?" );
	}
}