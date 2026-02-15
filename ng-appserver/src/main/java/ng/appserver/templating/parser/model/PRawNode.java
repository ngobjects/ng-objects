package ng.appserver.templating.parser.model;

import java.util.Objects;

/**
 * Represents a raw/verbatim block (<p:raw>...</p:raw>) whose content is NOT processed as template.
 * Included in the rendered output as-is.
 */

public record PRawNode( String value ) implements PNode {

	public PRawNode {
		Objects.requireNonNull( value );
	}
}
