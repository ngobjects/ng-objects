package ng.appserver.templating.parser.model;

import java.util.Objects;

/**
 * Represents a parser/developer comment (<p:comment>...</p:comment>).
 * Content is NOT processed as template and is stripped entirely from the rendered output.
 */

public record PCommentNode( String value ) implements PNode {

	public PCommentNode {
		Objects.requireNonNull( value );
	}
}
