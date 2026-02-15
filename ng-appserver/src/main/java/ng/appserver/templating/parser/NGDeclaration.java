package ng.appserver.templating.parser;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a declaration of a dynamic tag
 *
 * @param isInline true if the declaration was parsed from an inline tag
 * @param name The declaration's name (used to reference the declaration from the HTML template)
 * @param namespace The namespace of the tag (e.g. "wo" for <wo:String />, "ui" for <ui:Button />)
 * @param type The declaration's type (name of dynamic element or component)
 * @param bindings A Map of associations (bindings) on the declaration
 */

public record NGDeclaration( boolean isInline, String name, String namespace, String type, Map<String, NGBindingValue> bindings ) {

	/**
	 * Represents a binding value in a declaration or inline tag.
	 */
	public sealed interface NGBindingValue {

		/**
		 * A binding with a value, e.g. value="$name" or value = someKeyPath
		 *
		 * @param isQuoted true if the value was quoted in the source (WOD-style)
		 * @param value The binding value string
		 */
		record Value( boolean isQuoted, String value ) implements NGBindingValue {

			public Value {
				Objects.requireNonNull( value );
			}
		}

		/**
		 * A boolean (valueless) binding, representing an HTML boolean attribute.
		 * Its semantic meaning is solely its presence, e.g. "disabled" in {@code <my:Widget disabled />}
		 */
		record BooleanPresence() implements NGBindingValue {}
	}

	public NGDeclaration {
		Objects.requireNonNull( name );
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( type );
		Objects.requireNonNull( bindings );
	}
}