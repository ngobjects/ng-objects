package ng.appserver.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGDynamicGroup extends NGDynamicElement {

	/**
	 * The elements of this DynamicGroup
	 */
	private List<NGElement> _children;

	public NGDynamicGroup( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_children = new ArrayList<>();
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		for( final NGElement child : _children ) {
			child.appendToResponse( response, context );
		}
	}

	/**
	 * @return The child elements of this DynamicGroup
	 */
	public List<NGElement> children() {
		return _children;
	}
}