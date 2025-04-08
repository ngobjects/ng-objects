package ng.appserver.elements;

import java.util.List;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.NGStructuralElement;
import ng.appserver.templating.assications.NGAssociation;
import ng.kvc.NGKeyValueCoding.UnknownKeyException;
import ng.xperimental.NGErrorMessageElement;

/**
 * A common superclass for dynamic elements that have multiple children in our template element tree
 */

public class NGDynamicGroup extends NGDynamicElement implements NGStructuralElement {

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

	/**
	 * FIXME: Kind of don't like having this constructor around. Needs thinking // Hugi 2024-11-16
	 */
	public NGDynamicGroup( final String name, final Map<String, NGAssociation> associations, final List<NGElement> children ) {
		super( null, null, null );
		_children = children;
	}

	/**
	 * FIXME: OK, we're including this method, just to get rid of that constructor that I feel is getting in our way. The whole construction of groups is a little iffy at the moment // Hugi 2024-11-16
	 */
	public static NGDynamicGroup of( final List<NGElement> children ) {
		return new NGDynamicGroup( null, null, children );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		appendChildrenToResponse( response, context );
	}

	@Override
	public void appendStructureToResponse( NGResponse response, NGContext context ) {
		appendChildrenToResponse( response, context );
	}

	protected void appendChildrenToResponse( NGResponse response, NGContext context ) {
		if( !_children.isEmpty() ) {
			context.elementID().addBranch();

			for( final NGElement child : children() ) {
				if( shouldAppendToResponseInContext( context ) ) {
					// FIXME: Certainly butt-ugly element display of some element debugging info, but being able to display the elementID tree while debugging is kind of useful, so we want to build something from this // Hugi 2024-09-28
					final boolean appendElementTreeDebugInfo = false;

					if( appendElementTreeDebugInfo ) {
						final String elementDebugWrapper = """
								<span style="border: 1px solid red; margin: 0px; padding: 0px"><span style="margin: 0px; padding: 0px; background-color: rgba(255,0,0,0.5); overflow-x: hidden; font-size: 10px">%s</span>
								""".formatted( context.elementID() + " : " + child.getClass().getSimpleName() );
						response.appendContentString( elementDebugWrapper );
					}

					// FIXME: This try/catch clause is experimental, let's see how this works out and work from there // Hugi 2024-11-19
					try {
						child.appendToResponse( response, context );
					}
					catch( UnknownKeyException unknownKeyException ) {
						new NGErrorMessageElement( "Unknown key", child.getClass().getSimpleName(), unknownKeyException.getMessage() ).appendToResponse( response, context );
					}

					if( appendElementTreeDebugInfo ) {
						response.appendContentString( "</span>" );
					}
				}
				else if( child instanceof NGStructuralElement structuralChild ) {
					structuralChild.appendStructureToResponse( response, context );
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

	/**
	 * @return true if the context is currently working inside an updateContainer meant to be updated.
	 *
	 * FIXME: We should probably be caching some of this operation. Even if this isn't heavy, it's going to get invoked for every element on the page // Hugi 20224-07-15
	 * FIXME: We can make this more exact/performant by rendering structure only for the branch(es) containing the updateContainer(s) we're actually targeting // Hugi 2024-07-16
	 * FIXME: This method probably belongs on the context. Maybe. We're starting to accumulate some API cruft so we need to think through this whole "decide what to render"-thing to make sure it's totally nice and awesome // Hugi 2024-10-15
	 */
	public static boolean shouldAppendToResponseInContext( final NGContext context ) {

		// FIXME: Dont' forget; this is a temporary reprieve // Hugi 2024-10-09
		if( context.forceFullUpdate ) {
			return true;
		}

		// The list of containers to update is passed in to the request as a header
		final String containerIDToUpdate = context.targetedUpdateContainerID();

		// If no containers are specified, we're doing a full page render, so always perform appendToResponse()
		if( containerIDToUpdate == null ) {
			return true;
		}

		if( context.containingUpdateContainerIDs.contains( containerIDToUpdate ) ) {
			return true;
		}

		return false;
	}
}