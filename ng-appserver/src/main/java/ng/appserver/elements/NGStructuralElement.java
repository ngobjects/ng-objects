package ng.appserver.elements;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;

public interface NGStructuralElement {

	public void appendStructureToResponse( NGResponse response, NGContext context );
}
