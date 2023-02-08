package ng.appserver.elements;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

/**
 * A common superclass for dynamic elements that have multiple children in our template element tree
 */

public class NGDynamicGroup extends NGDynamicElement {

	/**
	 * The elements of this DynamicGroup
	 */
	private final List<NGElement> _children;

	/**
	 * Construct a new Dynamic Group from a content template.
	 *
	 * Note that in the case of an empty tag (i.e. <tag></tag>), contentTemplate will be null.
	 */
	public NGDynamicGroup( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		this( name, associations, childrenFromTemplate( contentTemplate ) );
	}

	public NGDynamicGroup( final String name, final Map<String, NGAssociation> associations, final List<NGElement> children ) {
		super( null, null, null );
		_children = children;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		appendChildrenToResponse( response, context );
	}

	private boolean shouldRender( NGContext context ) {
		final List<String> ucHeader = context.request().headers().get( "x-updatecontainerid" );

		System.out.println( ucHeader );

		if( ucHeader != null ) {
			if( !context.updateContainerIDs.contains( ucHeader ) ) {
				return false;
			}
		}

		return true;
	}

	protected void appendChildrenToResponse( NGResponse response, NGContext context ) {
		if( shouldRender( context ) ) {
			if( _children != null ) { // See mention of nullyness in the declaration of _children
				context.elementID().addBranch();

				for( final NGElement child : children() ) {
					child.appendToResponse( response, context );
					context.elementID().increment();
				}

				context.elementID().removeBranch();
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

			final int count = _children.size();

			for( int i = 0; i < count && actionResults == null; ++i ) {
				final NGElement child = _children.get( i );
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
		if( _children != null ) {
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

	/**
	 * @return The list of elements represented by [template]
	 */
	private static List<NGElement> childrenFromTemplate( final NGElement contentTemplate ) {

		// Null represents an empty container tag (i.e. no children).
		if( contentTemplate == null ) {
			return Collections.emptyList();
		}

		// If the contained template is another DynamicGroup, "unwrap it", i.e. just steal it's kids.
		if( contentTemplate instanceof NGDynamicGroup dg ) {
			return dg.children();
		}

		// If template is any other element, it's an only child.
		return List.of( contentTemplate );
	}
}