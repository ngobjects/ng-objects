package ng.appserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ng.appserver.privates.NGParsedURI;

/**
 * FIXME: This class still represents work in progress.
 *
 * Missing:
 * - cache keys
 * - cache timeout (tied to session perhaps?)
 * - watching if the cache is about to fill?
 * - replaceable cache implementation, so we don't write always write to memory?
 */

public class NGResourceRequestHandlerDynamic extends NGRequestHandler {

	private static Map<String, byte[]> _cacheMap = new HashMap<>();

	public static void push( final String id, final byte[] data ) {
		_cacheMap.put( id, data );
	}

	public static byte[] pop( final String id ) {
		return _cacheMap.remove( id );
	}

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		final Optional<String> resourceID = NGParsedURI.of( request.uri() ).getStringOptional( 1 );

		if( resourceID.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final byte[] resourceBytes = pop( resourceID.get() );

		// FIXME: How to handle this properly? User configurable? Just always a 404
		if( resourceBytes == null ) {
			return new NGResponse( "Resource '" + resourceID.get() + "' does not exist", 404 );
		}

		// FIXME: Get the correct resource type
		final String mimeType = "image/jpeg";

		// FIXME: Detect and set the correct response headers
		final NGResponse response = new NGResponse( resourceBytes, 200 );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resourceID.get() ) );
		response.setHeader( "Content-Type", mimeType );
		return response;
	}
}