package ng.appserver.templating;

import java.util.Map;
import java.util.Objects;

import ng.appserver.NGAssociation;

/**
 * Represents a declaration of a dynamic tag
 *
 * @param name The declaration's name (used to reference the declaration from the HTML template)
 * @param type The declaration's type (name of dynamic element or component)
 * @param associations A Map of associations (bindings) on the declaration
 */

public record NGDeclaration( String name, String type, Map<String, NGAssociation> associations ) {

	public NGDeclaration {
		Objects.requireNonNull( name );
		Objects.requireNonNull( type );
		Objects.requireNonNull( associations );
	}
}