package ng.appserver.templating;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;

/**
 * An element that doesn't render content, but adds structure to the element tree.
 * See implementing elements and you'll know what that means.
 */

public interface NGStructuralElement {

	public void appendStructureToResponse( NGResponse response, NGContext context );
}
