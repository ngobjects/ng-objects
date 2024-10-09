package ng.appserver.elements.ajax;

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

public class AjaxSubmitButton extends NGDynamicElement {

	private final NGAssociation _actionAssociation;
	private final NGAssociation _updateContainerIDAssociation;

	/**
	 * Stores associations that get passed through to the generated tag as attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public AjaxSubmitButton( String name, Map<String, NGAssociation> associations, NGElement contentTemplate ) {
		super( null, null, null );
		_additionalAssociations = new HashMap<>( associations );
		_actionAssociation = _additionalAssociations.remove( "action" );
		_updateContainerIDAssociation = _additionalAssociations.remove( "updateContainerID" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		// FIXME: We should be allowing for a form submission to be performed without an updateContainer update
		final String onclick = "ajaxSubmitButtonClick(this,%s)".formatted( updateContainerIDParameter( context ) );

		final Map<String, String> attributes = new HashMap<>();
		attributes.put( "type", "button" );
		attributes.put( "name", context.elementID().toString() );
		attributes.put( "onclick", onclick );

		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		final String htmlString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
		response.appendContentString( htmlString );
	}

	/**
	 * @return The id of the updateContainer
	 */
	private String updateContainerIDParameter( NGContext context ) {
		if( _updateContainerIDAssociation != null ) {
			final String updateContainerID = (String)_updateContainerIDAssociation.valueInComponent( context.component() );
			return "'%s'".formatted( updateContainerID );
		}

		return "null";
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {

		// FIXME: If this button is invoked without an action binding, it should perform the containing form's action
		if( request.formValues().get( context.elementID().toString() ) != null ) {
			return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
		}

		return super.invokeAction( request, context );
	}
}