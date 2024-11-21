package ng.appserver.templating.parser;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a declaration of a dynamic tag
 *
 * @param name The declaration's name (used to reference the declaration from the HTML template)
 * @param type The declaration's type (name of dynamic element or component)
 * @param associations A Map of associations (bindings) on the declaration
 */

public record NGDeclaration( boolean isInline, String name, String type, Map<String, NGBindingValue> bindings ) {

	public record NGBindingValue( boolean isQuoted, String value ) {

		public NGBindingValue {
			Objects.requireNonNull( value );
		}
	}

	public NGDeclaration {
		Objects.requireNonNull( name );
		Objects.requireNonNull( type );
		Objects.requireNonNull( bindings );
	}
}