package ng.appserver.templating.parser;

import java.util.Objects;

public record PHTMLNode( String value ) implements PNode {

	public PHTMLNode {
		Objects.requireNonNull( value );
	}
}