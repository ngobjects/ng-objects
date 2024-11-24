package ng.appserver.templating.parser.model;

import java.util.List;
import java.util.Objects;

public record PGroupNode( List<PNode> children ) implements PNode {

	public PGroupNode {
		Objects.requireNonNull( children );
	}
}