package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;

/**
 * FIXME: Implement
 * FIXME: The form's method should default to POST (once we can handle POST requests)
 */

public class NGForm extends NGDynamicGroup {

	private NGAssociation _actionAssociation;

	public NGForm( String name, Map<String, NGAssociation> associations, NGElement contentTemplate ) {
		super( name, associations, contentTemplate );
		_actionAssociation = associations.get( "action" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final Map<String, String> attributes = new HashMap<>();

		attributes.put( "method", "POST" );

		if( _actionAssociation != null ) {
			attributes.put( "action", context.componentActionURL() );
		}

		response.appendContentString( NGHTMLUtilities.createElementStringWithAttributes( "form", attributes, false ) );
		context.setIsInForm( true );
		appendChildrenToResponse( response, context );
		context.setIsInForm( false );
		response.appendContentString( "</form>" );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		if( context.elementID().toString().equals( context.senderID() ) ) {
			if( _actionAssociation != null ) {
				return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
			}
		}

		return null;
	}
}