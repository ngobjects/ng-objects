package ng.appserver;

import java.util.Objects;

/**
 * FIXME: Should we allow creation of components without a context?
 * FIXME: Templating really belongs in a separate project, right?
 */

public class NGComponent extends NGElement implements NGActionResults {

	private final NGContext _context;
	
	/**
	 * FIXME: Shouldn't this really be final and initialized during component construction? // Hugi 2022-01-16
	 * FIXME: This should definitely not be public
	 */
	public NGComponentDefinition _componentDefinition;

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
		// FIXME: We probably want to cache the template, just a question of if we do it here or elsewhere
		return _componentDefinition.template();
	}
}