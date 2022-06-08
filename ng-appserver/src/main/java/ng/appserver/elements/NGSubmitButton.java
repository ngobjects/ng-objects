package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGSubmitButton extends NGDynamicElement {

	public NGAssociation actionAssociation;

	public NGSubmitButton( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		actionAssociation = associations.get( "action" );

		if( actionAssociation == null ) {
			throw new IllegalArgumentException( "'action' is a required binding" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		// FIXME: Add a proper name/value based on the elementID so we can catch the button pressed for a later invocation of invokeAction()
		response.appendContentString( "<input type=\"submit\" name=\"hehe\">" );
	}
}