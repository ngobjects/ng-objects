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

	public static NGDeclaration create( final String declarationName, final String declarationType, final Map<String, NGAssociation> associations ) {
		Objects.requireNonNull( declarationName );
		Objects.requireNonNull( declarationType );
		Objects.requireNonNull( associations );
		return new NGDeclaration( declarationName, declarationType, associations );
	}
}