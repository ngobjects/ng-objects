package ng.appserver;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * NGComponentReference holds a reference to a component being rendered in the template tree.
 */

public class NGComponentReference extends NGDynamicElement {

	/**
	 * Holds a reference to the fully qualified class name of the component we're going to render
	 */
	private final String _name;

	/**
	 * The bindings being passed from the parent component to the component being rendered.
	 */
	private Map<String, NGAssociation> _associations;

	/**
	 * In the case of wrapper components, the content placed inside the component tags.
	 *
	 * <wo:SomeComponent>[contentTemplate]</wo:SomeComponent>
	 */
	private final NGElement _contentTemplate;

	public NGComponentReference( final String fullyQualifiedClassName, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		super( null, null, null );
		Objects.requireNonNull( fullyQualifiedClassName );
		Objects.requireNonNull( associations );
		_name = fullyQualifiedClassName;
		_associations = associations;
		_contentTemplate = contentTemplate;
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		// Let's take hold of component that's being rendered, before we hand control to the new component
		final NGComponent previousComponent = context.component();

		// Load up our component's definition
		// FIXME: We construct a component reference from a component definition. Shouldn't we have cached the definition at that stage?
		final NGComponentDefinition newComponent = NGApplication.application()._componentDefinition( _name, Collections.emptyList() );

		// Create an instance of the component
		// FIXME: In this case we might want to reuse instances of the components are stateless. But stateless components are not implemented yet, so...
		final NGComponent newComponentInstance = newComponent.componentInstanceInContext( context );

		newComponentInstance.setParent( previousComponent );
		newComponentInstance.setAssociations( _associations );
		newComponentInstance.setContentElement( _contentTemplate );

		// Set the component in the context
		context.setCurrentComponent( newComponentInstance );

		newComponentInstance.appendToResponse( response, context );

		// Return control to the previous component
		context.setCurrentComponent( previousComponent );
	}

	/**
	 * FIXME: Implement
	 */
	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		super.takeValuesFromRequest( request, context );
	}

	/**
	 * FIXME: Implement
	 */
	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return super.invokeAction( request, context );
	}
}