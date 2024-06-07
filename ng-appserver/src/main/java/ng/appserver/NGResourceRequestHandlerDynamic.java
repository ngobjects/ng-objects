package ng.appserver;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
	 * The default path prefix for this request handler
	 */
	public static final String DEFAULT_PATH = "/wd/";

	/**
	 * Storage of dynamic data.
	 *
	 * FIXME: This is currently just a regular HashMap, so we're storing resources indefinitely if they're never "popped" (i.e. read)
	 * We're going to have to think about how best to approach a solution to this problem, since different resources might need different cache "scopes".
	 * At first thought a request/context scoped cache sounds like a sensible default.
	 * Other scopes could be session/application wide. Or custom? Hmm.
	 * // Hugi 2023-02-17
	 */
	private static Map<String, NGDynamicResource> _cacheMap = new ConcurrentHashMap<>();

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

		if( resource == null ) {
			return new NGResponse( "Dynamic resource '" + resourceID.get() + "' does not exist", 404 );
		}

		final NGResponse response = new NGResponse();
		response.setStatus( 200 );
		response.setContentInputStream( resource.inputStream(), resource.length() );
		response.setHeader( "content-disposition", String.format( "inline;filename=\"%s\"", resource.filename() ) );
		response.setHeader( "Content-Type", resource.mimeType() );
		return response;
	}

	/**
	 * @return The URL for the named resource
	 *
	 * FIXME: Shouldn't be static
	 * FIXME: I don't feel this belongs here, URL generation will be dependent on the environment
	 */
	public static Optional<String> urlForDynamicResourceNamed( final String resourceName ) {
		Objects.requireNonNull( resourceName );

		final StringBuilder b = new StringBuilder();
		b.append( DEFAULT_PATH );
		b.append( resourceName );

		return Optional.of( b.toString() );
	}

	/**
	 * Represents a cached in-memory resource.
	 */
	public static record NGDynamicResource(
			InputStream inputStream,
			String filename,
			String mimeType,
			Long length ) {}
}