package ng.appserver;

import java.util.Map;
import java.util.Objects;

import ng.appserver.elements.NGStructuralElement;

/**
 * NGComponentReference holds a reference to a component being rendered in the template tree.
 */

public class NGComponentReference extends NGDynamicElement implements NGStructuralElement {

	/**
	 * Holds a reference to the fully qualified class name of the component we're going to render
	 */
	private final String _componentName;

	/**
	 * The bindings being passed from the parent component to the component being rendered.
	 */
	private Map<String, NGAssociation> _associations;

	/**
	 * In the case of wrapper components, the template wrapped by component (between the component opening/closing tags). If any.
	 */
	private final NGElement _contentTemplate;

	/**
	 * @param fullyQualifiedClassName Fully qualified componentclassname
	 * @param associations Associations used to initialize this component
	 * @param contentTemplate In the case of wrapper components, the template wrapped by component (between the component opening/closing tags)
	 *
	 * <wo:SomeComponent>[contentTemplate]</wo:SomeComponent>
	 */
	public NGComponentReference( final String componentName, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		super( null, null, null );
		Objects.requireNonNull( componentName );
		Objects.requireNonNull( associations );
		_componentName = componentName;
		_associations = associations;
		_contentTemplate = contentTemplate;
	}

	private void beforeComponent( final NGContext context ) {
		// Let's take hold of component that's being rendered, before we hand control to the new component
		final NGComponent previousComponent = context.component();

		// If we've already rendered a component at this location in the element tree, an instance should be cached under it's elementID in parent's children map
		NGComponent newComponentInstance = previousComponent.getChild( context.elementID().toString() );

		// CHECKME: If we actually did obtain an instance from the component's child cache, don't we need to set the child's context? // Hugi 2023-03-11
		// Update on that: So; since we should only be getting a cached page instance if the page was served by the component request handler,
		// the request handler has already updated the context for us.
		// However, I'm keeping this comment in (and downgrading to CHECKME) since this might become relevant when/if we implement stateless components

		// If no instance was obtained, we need to create the component instance and insert it into the parent's child map.
		if( newComponentInstance == null ) {
			// Load up our component's definition
			final NGComponentDefinition componentDefinition = NGApplication.application()._componentDefinition( _componentName );

			// ...and obtain an instance of the component
			newComponentInstance = componentDefinition.componentInstanceInContext( context );

			// Finally, we store our component instance in it's parent child map, ensuring we're reusing instances between requests
			previousComponent.addChild( context.elementID().toString(), newComponentInstance );
		}

		newComponentInstance.setParent( previousComponent, _associations, _contentTemplate );

		// Before we make our newly created component the "active" one, we need to pull values, if required
		newComponentInstance.pullBindingValuesFromParent();

		// Set the component in the context
		context.setComponent( newComponentInstance );
	}

	/**
	 * Return control to the previous component
	 */
	private void afterComponent( final NGContext context ) {
		context.component().pushBindingValuesToParent();
		context.setComponent( context.component().parent() );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		appendStructureToResponse( response, context );
	}

	@Override
	public void appendStructureToResponse( NGResponse response, NGContext context ) {
		beforeComponent( context );
		context.component().appendToResponse( response, context );
		afterComponent( context );
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		beforeComponent( context );
		context.component().takeValuesFromRequest( request, context );
		afterComponent( context );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		beforeComponent( context );
		NGActionResults result = context.component().invokeAction( request, context );
		afterComponent( context );
		return result;
	}
}