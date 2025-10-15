package ng.appserver.templating.parser.model;

import java.util.List;
import java.util.Objects;

public record PRootNode( List<PNode> children ) implements PNode {

	public PRootNode {
		Objects.requireNonNull( children );
	}
}