package ng.appserver.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGDynamicGroup extends NGDynamicElement {

	/**
	 * The elements of this DynamicGroup
	 */
	private final List<NGElement> _children;

	/**
	 * Construct a new Dynamic Group from template
	 *
	 * @param name
	 * @param associations
	 * @param template
	 */
	public NGDynamicGroup( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		super( null, null, null );

		if( template != null ) {
			if( template instanceof NGDynamicGroup dynamicGroup ) {
				_children = dynamicGroup.children();
			}
			else {
				_children = new ArrayList<>();
				_children.add( template );
			}
		}
		else {
			_children = new ArrayList<>();
		}
	}

	public NGDynamicGroup( final String name, final Map<String, NGAssociation> associations, final List<NGElement> children ) {
		super( null, null, null );
		Objects.requireNonNull( children );
		_children = children;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		appendChildrenToResponse( response, context );
	}

	protected void appendChildrenToResponse( NGResponse response, NGContext context ) {
		if( _children != null ) { // See mention of nullyness in the declaration of _children
			for( final NGElement child : children() ) {
				child.appendToResponse( response, context );
			}
		}
	}

	/**
	 * @return The child elements of this DynamicGroup
	 */
	public List<NGElement> children() {
		return _children;
	}
}