package ng.appserver.elements;

import java.util.HashMap;
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
	private NGAssociation _valueAssociation; // FIXME: This should just be passed through with other "generic" HTML attributes

	public NGSubmitButton( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_actionAssociation = associations.get( "action" );

		if( _actionAssociation == null ) {
			throw new IllegalArgumentException( "'action' is a required binding" );
		}

		_valueAssociation = associations.get( "value" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final Map<String, String> attributes = new HashMap<>();
		attributes.put( "type", "submit" );
		attributes.put( "name", context.elementID().toString() );

		if( _valueAssociation != null ) {
			attributes.put( "value", (String)_valueAssociation.valueInComponent( context.component() ) );
		}

		final String htmlString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
		response.appendContentString( htmlString );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		// FIXME: // Hugi 2023-02-02
		// This would be our regular way of checking for a senderID. HOWEVER....
		// if( context.elementID().toString().equals( context.senderID() ) ) {

		// So. The button is not actually the submitter, but rather the containing form.
		// The pressed submit button's name will be in the query parameter dictionary.
		// I have a feeling this is going to cause us grief with regard to state management, i.e. a single form can now result in different results. Exciting.
		if( request.formValues().get( context.elementID().toString() ) != null ) {
			return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
		}

		return super.invokeAction( request, context );
	}
}