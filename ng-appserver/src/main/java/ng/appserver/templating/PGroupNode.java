package ng.appserver.templating;

import java.util.List;
import java.util.Objects;

public record PGroupNode( List<Object> children ) implements PNode {

	public PGroupNode {
		Objects.requireNonNull( children );
	}
}