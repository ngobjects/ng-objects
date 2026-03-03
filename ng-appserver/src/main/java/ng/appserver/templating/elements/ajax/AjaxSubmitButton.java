package ng.appserver.templating.elements.ajax;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.elements.NGDynamicGroup;

public class AjaxSubmitButton extends NGDynamicGroup {

	private final NGAssociation _actionAssociation;
	private final NGAssociation _updateContainerIDAssociation;

	/**
	 * Stores associations that get passed through to the generated tag as attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public AjaxSubmitButton( String name, Map<String, NGAssociation> associations, NGElement contentTemplate ) {
		super( name, associations, contentTemplate );
		_additionalAssociations = new HashMap<>( associations );
		_actionAssociation = _additionalAssociations.remove( "action" );
		_updateContainerIDAssociation = _additionalAssociations.remove( "updateContainerID" );
	}

	/**
	 * @return true if this element has children, meaning it should render as a <button> wrapping its content
	 */
	private boolean hasChildren() {
		return !children().isEmpty();
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

		if( hasChildren() ) {
			// Render as <button>...children...</button>
			final String openTag = NGHTMLUtilities.createElementStringWithAttributes( "button", attributes, false );
			response.appendContentString( openTag );
			appendChildrenToResponse( response, context );
			response.appendContentString( "</button>" );
		}
		else {
			// Render as self-closing <input />
			final String htmlString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
			response.appendContentString( htmlString );
		}
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

		// If we have children, allow them to invoke their actions too
		if( hasChildren() ) {
			return invokeChildrenAction( request, context );
		}

		return null;
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		// If we have children, let them process form values
		if( hasChildren() ) {
			takeChildrenValuesFromRequest( request, context );
		}
	}
}
