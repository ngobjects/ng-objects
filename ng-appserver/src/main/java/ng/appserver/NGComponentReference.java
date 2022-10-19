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
	private final String _componentName;

	/**
	 * The bindings being passed from the parent component to the component being rendered.
	 */
	private Map<String, NGAssociation> _associations;

	/**
	 * @param fullyQualifiedClassName Fully qualified componentclassname
	 * @param associations Associations used to initialize this component
	 * @param contentTemplate In the case of wrapper components, the template wrapped by component (between the component opening/closing tags)
	 *
	 * <wo:SomeComponent>[contentTemplate]</wo:SomeComponent>
	 */
	private final NGElement _contentTemplate;

	public NGComponentReference( final String componentName, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		super( null, null, null );
		Objects.requireNonNull( componentName );
		Objects.requireNonNull( associations );
		_componentName = componentName;
		_associations = associations;
		_contentTemplate = contentTemplate;
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		// Let's take hold of component that's being rendered, before we hand control to the new component
		final NGComponent previousComponent = context.component();

		// Load up our component's definition
		final NGComponentDefinition newComponent = NGApplication.application()._componentDefinition( _componentName, Collections.emptyList() );

		// Obtain an instance of the component
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