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

	protected void appendChildrenToResponse( NGResponse response, NGContext context ) {
		if( !_children.isEmpty() ) {
			context.elementID().addBranch();

			for( final NGElement child : children() ) {
				if( shouldAppendToResponseInContext( context ) ) {
					child.appendToResponse( response, context );
				}
				else if( child instanceof NGDynamicGroup dg ) {
					dg.appendChildrenToResponse( response, context );
				}
				else if( child instanceof NGStructuralElement dg ) {
					dg.appendStructureToResponse( response, context );
				}

				context.elementID().increment();
			}

			context.elementID().removeBranch();
		}
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return invokeChildrenAction( request, context );
	}

	private NGActionResults invokeChildrenAction( NGRequest request, NGContext context ) {
		NGActionResults actionResults = null;

		if( !_children.isEmpty() ) {
			context.elementID().addBranch();

			// CHECKME: We might want to chek if this could cause problems during element structure changes
			// same as in NGRepetition's invokeAction()
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
		if( !_children.isEmpty() ) {
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
		// FIXME: Eliminate this check. The rendering engine should be using an empty list
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

	/**
	 * @return true if the context is currently working inside an updateContainer meant to be updated.
	 *
	 * FIXME: We should probably be caching some of this operation. Even if this isn't heavy, it's going to get invoked for every element on the page // Hugi 20224-07-15
	 * FIXME: We can make this more exact/performant by rendering structure only for the branch(es) containing the updateContainer(s) we're actually targeting // Hugi 2024-07-16
	 */
	private static boolean shouldAppendToResponseInContext( final NGContext context ) {

		// The list of containers to update is passed in to the request as a header
		final List<String> containerIDsToUpdate = context.request().headersForKey( "x-updatecontainerid" );

		// If no containers are specified, we're doing a full page render, so always perform appendToResponse()
		if( containerIDsToUpdate.isEmpty() ) {
			return true;
		}

		for( final String id : containerIDsToUpdate ) {
			if( context.updateContainerIDs.contains( id ) ) {
				return true;
			}
		}

		return false;
	}
}