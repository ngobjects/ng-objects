package ng.appserver.templating.elements;

import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGComponent;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.NGStructuralElement;
import ng.appserver.templating.associations.NGAssociation;

public class NGComponentContent extends NGDynamicElement implements NGStructuralElement {

	public NGComponentContent( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
	}

	@Override
	public void appendStructureToResponse( NGResponse response, NGContext context ) {
		final NGComponent component = context.component();

		if( component.contentElement() != null ) {
			context.setComponent( component.parent() );
			component.contentElement().appendOrTraverse( response, context );
			context.setComponent( component );
		}
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		final NGComponent component = context.component();

		if( component.contentElement() != null ) {
			context.setComponent( component.parent() );
			component.contentElement().takeValuesFromRequest( request, context );
			context.setComponent( component );
		}
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		NGActionResults actionResults = null;

		final NGComponent component = context.component();

		if( component.contentElement() != null ) {
			context.setComponent( component.parent() );
			actionResults = component.contentElement().invokeAction( request, context );
			context.setComponent( component );
		}

		return actionResults;
	}
}