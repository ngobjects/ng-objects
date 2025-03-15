package ng.appserver.elements.ajax;

import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.templating.assications.NGAssociation;

public class AjaxObserveField extends NGDynamicGroup {

	public AjaxObserveField( String name, Map<String, NGAssociation> associations, NGElement element ) {
		super( name, associations, element );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		response.appendContentString( "<div class=\"ng-observe-descendant-fields\">" );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</div>" );
	}
}