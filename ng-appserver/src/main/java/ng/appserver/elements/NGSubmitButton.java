package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;

/**
 * FIXME: This is clearly an element that needs to pass on it's standard HTML attributes (such as "value") // Hugi 2022-12-30
 */

public class NGSubmitButton extends NGDynamicElement {

	private NGAssociation _actionAssociation;

	public NGSubmitButton( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_actionAssociation = associations.get( "action" );

		if( _actionAssociation == null ) {
			throw new IllegalArgumentException( "'action' is a required binding" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final String htmlString = NGHTMLUtilities.createElementStringWithAttributes( "input", Map.of( "type", "submit", "name", context.elementID().toString() ), true );
		response.appendContentString( htmlString );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		if( context.elementID().toString().equals( context.senderID() ) ) {
			return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
		}

		return super.invokeAction( request, context );
	}
}