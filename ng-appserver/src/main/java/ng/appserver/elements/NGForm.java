package ng.appserver.elements;

import java.util.List;
import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

/**
 * FIXME: Implement
 */

public class NGForm extends NGDynamicGroup {

	public NGForm( String name, Map<String, NGAssociation> associations, List<NGElement> children ) {
		super( name, associations, children );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		appendChildrenToResponse( response, context );
	}
}