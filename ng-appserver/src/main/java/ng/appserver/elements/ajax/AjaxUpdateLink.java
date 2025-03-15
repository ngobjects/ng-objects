package ng.appserver.elements.ajax;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGBindingConfigurationException;

public class AjaxUpdateLink extends NGDynamicGroup {

	private final NGAssociation _actionAssociation;
	private final NGAssociation _updateContainerIDAssociation;

	/**
	 * Stores associations that get passed through to the generated tag as attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public AjaxUpdateLink( String name, Map<String, NGAssociation> associations, NGElement element ) {
		super( name, associations, element );
		_additionalAssociations = new HashMap<>( associations );
		_actionAssociation = _additionalAssociations.remove( "action" );
		_updateContainerIDAssociation = _additionalAssociations.remove( "updateContainerID" );

		if( _actionAssociation == null ) {
			throw new NGBindingConfigurationException( "[action] is a required binding" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		final String onclick = "ajaxUpdateLinkClick('%s',%s);return false;".formatted( context.componentActionURL(), updateContainerIDParameter( context ) );

		final Map<String, String> attributes = new HashMap<>();
		attributes.put( "href", "#" );
		attributes.put( "onclick", onclick );

		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		final String htmlString = NGHTMLUtilities.createElementStringWithAttributes( "a", attributes, false );
		response.appendContentString( htmlString );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</a>" );
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

		if( context.elementID().equals( context.senderID() ) ) {
			return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
		}

		return null;
	}
}