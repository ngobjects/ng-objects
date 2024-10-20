package ng.appserver.templating;

import java.util.Objects;

public record PBasicNode( NGDynamicHTMLTag tag ) implements PNode {

	public PBasicNode {
		Objects.requireNonNull( tag );
	}
}