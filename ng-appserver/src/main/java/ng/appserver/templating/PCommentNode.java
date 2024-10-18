package ng.appserver.templating;

import java.util.Objects;

public record PCommentNode( String value ) implements PNode {

	public PCommentNode {
		Objects.requireNonNull( value );
	}
}