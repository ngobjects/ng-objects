package ng.appserver.templating.elements;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;

/**
 * An HTML submit button, specifically useful for submitting/invoking component actions
 */

public class NGSubmitButton extends NGDynamicElement {

	/**
	 * The action to invoke when this button is pressed
	 */
	private final NGAssociation _actionAssociation;

	/**
	 * Pass-through attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGSubmitButton( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_additionalAssociations = new HashMap<>( associations );

		_actionAssociation = _additionalAssociations.remove( "action" );

		if( _actionAssociation == null ) {
			throw new IllegalArgumentException( "'action' is a required binding" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final Map<String, String> attributes = new HashMap<>();
		attributes.put( "type", "submit" );
		attributes.put( "name", context.elementID().toString() );

		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		final String htmlString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
		response.appendContentString( htmlString );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {

		// This would be our regular way of checking for a senderID:
		// if( context.currentElementIsSender() ) {...}
		// ....HOWEVER....
		// The pressed button is not actually the submitting element, the form that contains the button is.
		// The pressed submit button's name will be in the query parameter dictionary, so that's what we check for.
		// I have a feeling this is going to cause us grief with regard to state management, i.e. the same form/elementID can now result in different actions being invoked. Very exciting.
		// CHECKME: This might actually be a perfect application for the formaction attributes (which would allow for a more traditional handling of the invoked button) // Hugi 2024-06-01
		if( request.formValues().get( context.elementID().toString() ) != null ) {
			return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
		}

		return super.invokeAction( request, context );
	}
}