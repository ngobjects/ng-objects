package ng.appserver;

import java.util.Objects;
import java.util.Optional;

import ng.appserver.privates.NGParsedURI;
import ng.appserver.resources.NGDynamicResource;

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

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		final String resourceID = NGParsedURI.of( request.uri() ).getString( 1 );

		if( resourceID == null ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final NGDynamicResource resource = NGApplication.application().resourceManagerDynamic().pop( resourceID );

		if( resource == null ) {
			return new NGResponse( "Dynamic resource '" + resourceID + "' does not exist", 404 );
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
}