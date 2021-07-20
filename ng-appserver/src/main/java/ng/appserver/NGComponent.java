package ng.appserver;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.privates.NGUtils;

/**
 * FIXME: Should we allow creation of components without a context?
 * FIXME: Templating really belongs in a separate project, right?
 */

public class NGComponent extends NGElement implements NGActionResults {

	private static final Logger logger = LoggerFactory.getLogger( NGComponent.class );

	private final NGContext _context;

	/**
	 * Names of component templates end with this
	 */
	private static final String COMPONENT_TEMPLATE_SUFFIX = "ngml";

	public NGComponent( final NGContext context ) {
		Objects.requireNonNull( context );
		_context = context;
	}

	@Override
	public NGResponse generateResponse() {
		final NGResponse response = new NGResponse();
		response.setHeader( "content-type", "text/html;charset=utf-8" ); // FIXME: This is most definitely not the place to set the encoding
		appendToResponse( response, null );
		return response;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final NGComponent previouslyCurrentComponent = context.component();
		context.setCurrentComponent( this );
		template().appendToResponse( response, context );
		context.setCurrentComponent( previouslyCurrentComponent );
	}

	public NGElement template() {
		final String htmlTemplateFilename = getClass().getSimpleName() + "." + COMPONENT_TEMPLATE_SUFFIX;
		final String htmlTemplatePath = NGUtils.resourcePath( "components", htmlTemplateFilename );

		final Optional<byte[]> templateBytes = NGUtils.readJavaResource( htmlTemplatePath );

		if( templateBytes.isEmpty() ) {
			throw new RuntimeException( "Template not found" );
		}

		final String templateString = new String( templateBytes.get(), StandardCharsets.UTF_8 );
		return new NGHTMLBareString( templateString );
	}
}