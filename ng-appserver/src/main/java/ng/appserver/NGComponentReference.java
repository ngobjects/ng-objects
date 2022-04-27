package ng.appserver;

import java.util.Collections;
import java.util.Map;

public class NGComponentReference extends NGDynamicElement {

	private final String _name;
	private Map<String, NGAssociation> _associations;
	private final NGElement _template;

	public NGComponentReference( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_name = name;
		_associations = associations;
		_template = template;
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		// First we need to keep the context going
		final NGComponent previousComponent = context.component();

		// Load up our component's definition
		final NGComponentDefinition newComponent = NGApplication.application()._componentDefinition( _name, Collections.emptyList() );

		final NGComponent newComponentInstance = newComponent.componentInstanceInstanceInContext( context );

		// Set the component in the context
		context.setCurrentComponent( newComponentInstance );

		newComponentInstance.appendToResponse( response, context );

		// Return control to the previous component
		context.setCurrentComponent( previousComponent );
	}
}
