package ng.appserver.templating.parser.model;

import java.util.Objects;

/**
 * A template/developer comment (<!--# ... -->) whose content is NOT processed as template.
 * Stripped entirely from the rendered output.
 */

public record PTemplateComment( String value ) implements PNode {

	public PTemplateComment {
		Objects.requireNonNull( value );
	}
}
