package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;

/**
 * An HTML form
 */

public class NGForm extends NGDynamicGroup {

	/**
	 * The action that will be performed by this form
	 */
	private final NGAssociation _actionAssociation;

	/**
	 * The action that will be performed by this form
	 */
	private final NGAssociation _methodAssociation;

	/**
	 * Pass-through attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGForm( String name, Map<String, NGAssociation> associations, NGElement contentTemplate ) {
		super( name, associations, contentTemplate );
		_additionalAssociations = new HashMap<>( associations );

		_actionAssociation = _additionalAssociations.remove( "action" );
		_methodAssociation = _additionalAssociations.remove( "method" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		// Attributes to be added to the generated HTML-tag
		final Map<String, String> attributes = new HashMap<>();

		String method;

		if( _methodAssociation != null ) {
			method = (String)_methodAssociation.valueInComponent( context.component() );
		}
		else {
			method = "POST";
		}

		attributes.put( "method", method );

		// We append the action association, even if there's no action bound.
		// This is due to forms with multiple submit buttons, see invokeAction() for further documentation
		// CHECKME: We're going to have to revisit this for potential direct action submissions/route submissions // Hugi 2023-04-15
		attributes.put( "action", context.componentActionURL() );

		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

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
			// CHECKME: A nice feature might be if we could make the action bound to the form the default action to perform on form submission, if no submit button was pressed (for example, if the return button was pressed) // Hugi 2023-04-15
			if( _actionAssociation != null ) {
				return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
			}
			else {
				// In this case, a button in the form was targeted, so we  keep on looking down the tree, allowing NGSubmitButton's invokeAction() logic to catch the click
				// CHECKME: This currently feels redundant, since this method will currently end up invoking invokeChildrenAction() anyway. But I like having this explicit while we're still fleshing out the design (which still needs improvement) // Hugi 2024-09-28
				return super.invokeAction( request, context );
			}
		}

		return super.invokeAction( request, context );
	}
}