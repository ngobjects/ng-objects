package ng.appserver.templating.parser;

import java.util.Objects;

public record PCommentNode( String value ) implements PNode {

	public PCommentNode {
		Objects.requireNonNull( value );
	}
}