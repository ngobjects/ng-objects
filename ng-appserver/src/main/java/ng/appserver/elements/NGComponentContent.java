package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.templating.assications.NGAssociation;

public class NGComponentContent extends NGDynamicElement implements NGStructuralElement {

	public NGComponentContent( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		appendStructureToResponse( response, context );
	}

	@Override
	public void appendStructureToResponse( NGResponse response, NGContext context ) {
		final NGComponent component = context.component();

		if( component.contentElement() != null ) {
			context.setComponent( component.parent() );
			component.contentElement().appendToResponse( response, context );
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