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

	public NGComponent( final NGContext context ) {
		Objects.requireNonNull( context );
		_context = context;
	}

	public NGContext context() {
		return _context;
	}

	@Override
	public NGResponse generateResponse() {
		final NGResponse response = new NGResponse();
		response.setHeader( "content-type", "text/html;charset=utf-8" ); // FIXME: This is most definitely not the place to set the encoding
		appendToResponse( response, context() );
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
		return parseTemplate( getClass().getSimpleName() );
	}

	public static NGElement parseTemplate( final String templateName ) {
		final String htmlTemplateFilename = templateName + "/" + templateName + ".html";
		final String htmlTemplatePath = NGUtils.resourcePath( "components", htmlTemplateFilename );

		final Optional<byte[]> templateBytes = NGUtils.readJavaResource( htmlTemplatePath );

		if( templateBytes.isEmpty() ) {
			throw new RuntimeException( "Template not found" );
		}

		final String templateString = new String( templateBytes.get(), StandardCharsets.UTF_8 );
		return new NGHTMLBareString( templateString );
	}
}