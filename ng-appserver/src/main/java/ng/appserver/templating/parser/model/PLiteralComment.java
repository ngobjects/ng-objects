package ng.appserver.templating.parser.model;

import java.util.Objects;

/**
 * A literal comment (<!--! ... -->) whose content is NOT processed as template.
 * Included in the rendered output as a regular HTML comment.
 */

public record PLiteralComment( String value ) implements PNode {

	public PLiteralComment {
		Objects.requireNonNull( value );
	}
}
