package ng.appserver.elements.ajax;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;
import ng.appserver.elements.NGDynamicGroup;

public class AjaxObserveField extends NGDynamicGroup {

	public AjaxObserveField( String name, Map<String, NGAssociation> associations, NGElement element ) {
		super( name, associations, element );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		response.appendContentString( "<div class=\"ng-ajax-observe-field\">" );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</div>" );
	}
}