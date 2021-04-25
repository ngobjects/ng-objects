package ng.appserver;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.privates.NGUtils;

/**
 * FIXME: Should we allow creation of components without a context?
 * FIXME: Templating really belongs in a separate project, right?
 */

public class NGComponent extends NGElement implements NGActionResults {

	private static final Logger logger = LoggerFactory.getLogger( NGComponent.class );

	/**
	 * Names of component templates end with this
	 */
	private static final String COMPONENT_TEMPLATE_SUFFIX = "ngml";

	public NGComponent( final NGContext context ) {
		
	}

	/**
	 * FIXME: Return an Optional, return null or throw an exception on no resource?
	 */
	public Optional<byte[]> bytesForResourceWithName( final String resourceName ) {
		final String resourcePath = "/app-resources/" + resourceName;
		
		logger.info( "Loading resource {} at path {}", resourceName, resourcePath );

		return NGUtils.readJavaResource( resourceName );
	}

	@Override
	public NGResponse generateResponse() {
		final Optional<byte[]> templateBytes = NGUtils.readJavaResource( "/components/" + getClass().getSimpleName() + "." + COMPONENT_TEMPLATE_SUFFIX );
		
		if( templateBytes.isEmpty() ) {
			throw new RuntimeException( "Template not found" );
		}

		final String templateString = new String( templateBytes.get(), StandardCharsets.UTF_8 );
		
		final var response = new NGResponse( templateString, 200 );
		response.setHeader( "content-type", "text/html;charset=utf-8" ); // FIXME: This is most definitely not the place to set the encoding
		return response;
	}
}