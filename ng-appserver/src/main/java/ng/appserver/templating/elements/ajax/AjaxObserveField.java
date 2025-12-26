package ng.appserver.templating.elements.ajax;

import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.elements.NGDynamicGroup;

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