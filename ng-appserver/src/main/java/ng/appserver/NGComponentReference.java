package ng.appserver;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * NGComponentReference holds a reference to a component being rendered in the template tree.
 */

public class NGComponentReference extends NGDynamicElement {

	/**
	 * Holds a reference to the fully qualified classname of the component we're going to render
	 */
	private final String _name;

	/**
	 * The bindings being passed from the parent component to the component being rendered.
	 */
	private Map<String, NGAssociation> _associations;

	private final NGElement _contentTemplate;

	public NGComponentReference( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		super( null, null, null );
		Objects.requireNonNull( name );
		Objects.requireNonNull( associations );
		_name = name;
		_associations = associations;
		_contentTemplate = contentTemplate;
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		// Let's take hold of component that's being rendered, before we hand control to the new component
		final NGComponent previousComponent = context.component();

		// Load up our component's definition
		final NGComponentDefinition newComponent = NGApplication.application()._componentDefinition( _name, Collections.emptyList() );

		// Create an instance of the component
		// FIXME: In this case we might want to reuse instances of the components are stateless. But stateless components are not implemented yet, so...
		final NGComponent newComponentInstance = newComponent.componentInstanceInstanceInContext( context );

		newComponentInstance.setParent( previousComponent );
		newComponentInstance.setAssociations( _associations );
		newComponentInstance.setContentElement( _contentTemplate );

		// Set the component in the context
		context.setCurrentComponent( newComponentInstance );

		newComponentInstance.appendToResponse( response, context );

		// Return control to the previous component
		context.setCurrentComponent( previousComponent );
	}
}
