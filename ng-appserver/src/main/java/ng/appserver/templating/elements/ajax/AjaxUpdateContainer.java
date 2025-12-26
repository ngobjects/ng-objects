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

public class AjaxUpdateContainer extends NGDynamicGroup {

	public NGAssociation _elementNameAssociation;
	public NGAssociation _idAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public AjaxUpdateContainer( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_additionalAssociations = new HashMap<>( associations );

		_elementNameAssociation = _additionalAssociations.remove( "elementName" );
		_idAssociation = _additionalAssociations.remove( "id" );
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		super.takeValuesFromRequest( request, context );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return super.invokeAction( request, context );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final String id = id( context );
		final StringBuilder b = new StringBuilder();

		String elementName = "div";

		if( _elementNameAssociation != null ) {
			elementName = (String)_elementNameAssociation.valueInComponent( context.component() );
		}

		final Map<String, String> attributes = new HashMap<>();
		attributes.put( "id", id );
		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		b.append( NGHTMLUtilities.createElementStringWithAttributes( elementName, attributes, false ) );
		response.appendContentString( b.toString() );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</div>" );
	}

	public String id( NGContext context ) {
		return (String)_idAssociation.valueInComponent( context.component() );
	}

	@Override
	protected void appendChildrenToResponse( NGResponse response, NGContext context ) {
		final String id = id( context );
		context.containingUpdateContainerIDs().add( id );
		super.appendChildrenToResponse( response, context );
		context.containingUpdateContainerIDs().remove( id );
	}
}