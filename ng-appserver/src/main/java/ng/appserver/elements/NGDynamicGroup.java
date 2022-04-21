package ng.appserver.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import er.extensions.bettertemplates.NSMutableArray;
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
		this._initChildrenFromTemplate( template );
		System.out.println( "bla" + " : " + name + " : " + associations + " : " + template );
	}

	public NGDynamicGroup( String _name, Object associations, NSMutableArray nsmutablearray ) {
		this( _name, (Map<String, NGAssociation>)associations, (NGElement)null );
		_children = nsmutablearray;
		System.out.println( "smu" + " : " + _name + " : " + associations + " : " + nsmutablearray );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		appendChildrenToResponse( response, context );
	}

	protected void appendChildrenToResponse( NGResponse response, NGContext context ) {
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

	private void _initChildrenFromTemplate( NGElement template ) {
		List<NGElement> children = null;

		if( template != null ) {
			if( template.getClass() == NGDynamicGroup.class ) {
				children = ((NGDynamicGroup)template).children();
			}
			else {
				children = new ArrayList<>();
				children.add( template );
			}
		}

		this._children = null;

		if( children != null && children.size() > 0 ) {
			this._children = children;
		}
	}
}