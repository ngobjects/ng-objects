package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

/**
 * FIXME: We need to handle the cases of invokeAction and takeValuesFromRequest here as well // Hugi 2022-06-10
 */

public class NGComponentContent extends NGDynamicElement {

	public NGComponentContent( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		super.appendToResponse( response, context );

		final NGComponent component = context.component();

		// FIXME: We also need to append the content of the component itself
		if( component.contentElement() != null ) {
			context.setCurrentComponent( component.parent() );
			component.contentElement().appendToResponse( response, context );
			context.setCurrentComponent( component );
		}
	}
}