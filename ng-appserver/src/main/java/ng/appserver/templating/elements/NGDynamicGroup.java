package ng.appserver.templating.elements;

import java.util.List;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.NGStructuralElement;
import ng.appserver.templating.associations.NGAssociation;

/**
 * A common superclass for dynamic elements that have multiple children in our template element tree
 */

public class NGDynamicGroup extends NGDynamicElement implements NGStructuralElement {

	/**
	 * The elements of this DynamicGroup
	 */
	private final List<NGElement> _children;

	/**
	 * Construct a new group from a content template
	 */
	public NGDynamicGroup( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		this( childrenFromTemplate( contentTemplate ) );
	}

	/**
	 * Construct a new group from an element list
	 */
	public NGDynamicGroup( final List<NGElement> children ) {
		super( null, null, null );
		_children = children;
	}

	@Override
	public void appendStructureToResponse( NGResponse response, NGContext context ) {
		appendChildrenToResponse( response, context );
	}

	protected void appendChildrenToResponse( NGResponse response, NGContext context ) {
		if( !_children.isEmpty() ) {
			context.elementID().addBranch();

			for( final NGElement child : children() ) {
				child.appendOrTraverse( response, context );
				context.elementID().increment();
			}

			context.elementID().removeBranch();
		}
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return invokeChildrenAction( request, context );
	}

	protected NGActionResults invokeChildrenAction( NGRequest request, NGContext context ) {
		NGActionResults actionResults = null;

		if( !_children.isEmpty() ) {
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

	protected void takeChildrenValuesFromRequest( NGRequest request, NGContext context ) {
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

		// If the contained template is another DynamicGroup, "unwrap it", i.e. just steal it's kids.
		if( contentTemplate.getClass().equals( NGDynamicGroup.class ) ) {
			return ((NGDynamicGroup)contentTemplate).children();
		}

		// If template is any other element, it's an only child.
		return List.of( contentTemplate );
	}
}