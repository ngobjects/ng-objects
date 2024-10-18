package ng.appserver.templating;

import java.util.Objects;

public record PHTMLNode( String value ) implements PNode {

	public PHTMLNode {
		Objects.requireNonNull( value );
	}
}