package ng.appserver.templating.parser.model;

import java.util.List;
import java.util.Objects;

/**
 * An HTML comment whose content is parsed as template (dynamic tags inside are processed).
 * Included in the rendered output wrapped in <!-- ... -->
 */

public record PHTMLComment( List<PNode> children ) implements PNode {

	public PHTMLComment {
		Objects.requireNonNull( children );
	}
}
