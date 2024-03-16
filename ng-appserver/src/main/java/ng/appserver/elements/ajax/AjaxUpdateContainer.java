package ng.appserver.elements.ajax;

import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.privates.NGHTMLUtilities;

public class AjaxUpdateContainer extends NGDynamicGroup {

	public NGAssociation _idAssociation;

	public AjaxUpdateContainer( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_idAssociation = associations.get( "id" );
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

		final String id = (String)_idAssociation.valueInComponent( context.component() );
		context.updateContainerIDs.add( id );

		final StringBuilder b = new StringBuilder();

		b.append( NGHTMLUtilities.createElementStringWithAttributes( "div", Map.of( "id", id ), false ) );
		response.appendContentString( b.toString() );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</div>" );
		context.updateContainerIDs.remove( id );
	}
}