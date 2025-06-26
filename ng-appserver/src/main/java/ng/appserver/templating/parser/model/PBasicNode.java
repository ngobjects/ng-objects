package ng.appserver.templating.parser.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.templating.parser.NGDeclaration.NGBindingValue;

public record PBasicNode( String type, Map<String, NGBindingValue> bindings, List<PNode> children, boolean isInline, String declarationName ) implements PNode {

	public PBasicNode {
		Objects.requireNonNull( type );
		Objects.requireNonNull( bindings );
		Objects.requireNonNull( children );
		Objects.requireNonNull( declarationName );
	}
}