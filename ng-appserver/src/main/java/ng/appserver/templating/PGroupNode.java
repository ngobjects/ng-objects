package ng.appserver.templating;

import java.util.Objects;

public record PGroupNode( NGDynamicHTMLTag tag ) implements PNode {

	public PGroupNode {
		Objects.requireNonNull( tag );
	}
}