package ng.appserver.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGDynamicGroup extends NGDynamicElement {

	/**
	 * The elements of this DynamicGroup
	 */
	private final List<NGElement> _children;

	/**
	 * Construct a new Dynamic Group from a content template
	 */
	public NGDynamicGroup( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		this( name, associations, childrenFromTemplate( template ) );
	}

	public NGDynamicGroup( final String name, final Map<String, NGAssociation> associations, final List<NGElement> children ) {
		super( null, null, null );
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

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return invokeChildrenAction( request, context );
	}

	private NGActionResults invokeChildrenAction( NGRequest request, NGContext context ) {
		NGActionResults actionResults = null;

		if( _children != null ) { // See mention of nullyness in the declaration of _children
			context.elementID().addBranch();

			for( final NGElement child : children() ) {
				actionResults = child.invokeAction( request, context );
				context.elementID().increment();
			}

			context.elementID().removeBranch();
		}

		return actionResults;
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		takeChildrenValuesFromRequest( request, context );
	}

	private void takeChildrenValuesFromRequest( NGRequest request, NGContext context ) {
		if( _children != null ) { // See mention of nullyness in the declaration of _children
			context.elementID().addBranch();

			for( final NGElement child : children() ) {
				child.takeValuesFromRequest( request, context );
				context.elementID().increment();
			}

			context.elementID().removeBranch();
		}
	}

	/**
	 * @return The child elements of this DynamicGroup
	 */
	public List<NGElement> children() {
		return _children;
	}

	private static List<NGElement> childrenFromTemplate( final NGElement template ) {
		if( template == null ) {
			return new ArrayList<>();
		}

		if( template instanceof NGDynamicGroup dg ) {
			return dg.children();
		}

		List<NGElement> children = new ArrayList<>();
		children.add( template );
		return children;
	}
}