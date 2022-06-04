package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGComponentContent extends NGDynamicElement {

	public NGComponentContent( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		super.appendToResponse( response, context );
	}
}
