package ng.appserver;

import java.util.Optional;

import ng.appserver.privates.NGParsedURI;

public class NGResourceRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( final NGRequest request ) {
		final Optional<String> resourceName = NGParsedURI.of( request.uri() ).elementAt( 1 );

		if( resourceName.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final Optional<byte[]> resourceBytes = NGApplication.application().resourceManager().bytesForResourceWithName( resourceName.get() );

		// FIXME: How to handle this properly? User configurable? Just always a 404
		if( resourceBytes.isEmpty() ) {
			return new NGResponse( "Resource '" + resourceName.get() + "' does not exist", 404 );
		}

		return new NGResponse( resourceBytes.get(), 200 );
	}
}