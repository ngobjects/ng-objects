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
 * An HTML form
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

		// We append the action association, even if there's no action bound.
		// This is due to forms with multiple submit buttons, see invokeAction() for further documentation
		attributes.put( "action", context.componentActionURL() );

		response.appendContentString( NGHTMLUtilities.createElementStringWithAttributes( "form", attributes, false ) );
		context.setIsInForm( true );
		appendChildrenToResponse( response, context );
		context.setIsInForm( false );
		response.appendContentString( "</form>" );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		if( context.currentElementIsSender() ) {

			// We only invoke the action association if the action binding is actually bound.
			// This is because the form might contain several submit buttons, in which case the actual action to invoke is the action of the button pressed.
			// The actual button pressed will be identified by the button's name being present in the request's formValues. See NGSubmitButton.invokeAction() for implementation details.
			// CHECKME: Having a bound action in a form which has a submit button, also with a bound action, might be an error condition // Hugi 2023-02-02
			if( _actionAssociation != null ) {
				return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
			}
			else {
				return super.invokeAction( request, context );
			}
		}

		return null;
	}
}