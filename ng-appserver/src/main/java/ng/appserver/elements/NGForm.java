package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

/**
 * FIXME: Implement
 * FIXME: The form's method should default to POST (once we can handle POST requests)
 */

public class NGForm extends NGDynamicGroup {

	public NGForm( String name, Map<String, NGAssociation> associations, NGElement contentTemplate ) {
		super( name, associations, contentTemplate );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		response.appendContentString( "<form>" );
		context.setIsInForm( true );
		appendChildrenToResponse( response, context );
		context.setIsInForm( false );
		response.appendContentString( "</form>" );
	}
}