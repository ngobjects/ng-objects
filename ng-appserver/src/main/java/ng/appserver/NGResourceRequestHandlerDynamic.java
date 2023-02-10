package ng.appserver;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.privates.NGParsedURI;

/**
 * Missing:
 * - cache keys
 * - cache timeout (tied to session perhaps?)
 * - watching if the cache is about to fill?
 * - replaceable cache implementation, so we don't write always write to memory?
 */

public class NGResourceRequestHandlerDynamic extends NGRequestHandler {

	/**
	 * Storage of dynamic data.
	 *
	 * FIXME: This is currently just a regular HashMap, so we're storing resources indefinitely if they're never "popped" (i.e. read)
	 */
	private static Map<String, NGDynamicResource> _cacheMap = new HashMap<>();

	public static void push( final String cacheKey, final NGDynamicResource data ) {
		Objects.requireNonNull( cacheKey );
		_cacheMap.put( cacheKey, data );
	}

	public static NGDynamicResource pop( final String cacheKey ) {
		return _cacheMap.remove( cacheKey );
	}

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		final Optional<String> resourceID = NGParsedURI.of( request.uri() ).getStringOptional( 1 );

		if( resourceID.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final NGDynamicResource resource = pop( resourceID.get() );

		// FIXME: How to handle this properly? User configurable? Just always a 404
		if( resource == null ) {
			return new NGResponse( "Resource '" + resourceID.get() + "' does not exist", 404 );
		}

		// FIXME: Get the correct resource type
		final String mimeType = "image/jpeg";

		// FIXME: Detect and set the correct response headers
		final NGResponse response = new NGResponse();
		response.setStatus( 200 );
		response.setContentInputStream( resource.inputStream() );
		response.setContentInputStreamLength( resource.length() );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resourceID.get() ) );
		response.setHeader( "Content-Type", mimeType );
		return response;
	}

	/**
	 * FIXME: This could also benefit from a filename... // Hugi 2023-02-10
	 */
	public static record NGDynamicResource(
			InputStream inputStream,
			String mimeType,
			Integer length ) {}
}