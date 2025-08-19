package ng.appserver.templating;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;

/**
 * An element that doesn't render content, but adds structure to the element tree.
 * See implementing elements and you'll know what that means.
 */

public interface NGStructuralElement extends NGElement {

	@Override
	public default void appendToResponse( NGResponse response, NGContext context ) {
		appendStructureToResponse( response, context );
	}

	public void appendStructureToResponse( NGResponse response, NGContext context );
}
