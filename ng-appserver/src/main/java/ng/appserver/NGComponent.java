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

	/**
	 * Stores a reference to the component's parent component
	 */
	private NGComponent _parent;

	public NGComponent( final NGContext context ) {
		Objects.requireNonNull( context );
		_context = context;
	}

	public NGContext context() {
		return _context;
	}

	/**
	 * @return The value of the named binding/association.
	 *
	 * FIXME: Not implemented
	 */
	public Object valueForBinding( String bindingName ) {
		return "Not implemented";
	}

	public void setParent( NGComponent parent ) {
		_parent = parent;
	}

	public NGComponent parent() {
		return _parent;
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

	/**
	 * FIXME: We probably want to cache the template, just a question of if we do it here or elsewhere // Hugi 2022-04-18
	 */
	public NGElement template() {
		return _componentDefinition.template();
	}
}