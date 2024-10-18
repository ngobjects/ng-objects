package ng.appserver.templating;

import java.util.Objects;

public record PBasicNode( NGDynamicHTMLTag tag, NGDeclaration declaration ) implements PNode {

	public PBasicNode {
		Objects.requireNonNull( tag );
		//		Objects.requireNonNull( declaration );
	}
}