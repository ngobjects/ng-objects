package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGTextField extends NGDynamicElement {

	public NGTextField( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		// FIXME: Using String.format for convenience. We probably want to change that later for performance reasons // Hugi 2022-06-05
		String tagString = String.format( "<input type=\"text\" name=\"%s\" />", name() );
		response.appendContentString( tagString );

	}

	private Object name() {
		return null;
	}
}