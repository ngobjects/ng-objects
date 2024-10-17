package ng.appserver.templating;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGAssociation;
import ng.appserver.NGAssociationFactory;

/**
 * Represents a declaration of a dynamic tag
 *
 * @param name The declaration's name (used to reference the declaration from the HTML template)
 * @param type The declaration's type (name of dynamic element or component)
 * @param associations A Map of associations (bindings) on the declaration
 */

public record NGDeclaration( boolean isInline, String name, String type, Map<String, NGBindingValue> bindings ) {

	public NGDeclaration {
		Objects.requireNonNull( name );
		Objects.requireNonNull( type );
		Objects.requireNonNull( bindings );
	}

	public Map<String, NGAssociation> associations() {
		final Map<String, NGAssociation> associations = new HashMap<>();

		for( Entry<String, NGBindingValue> entry : bindings.entrySet() ) {
			associations.put( entry.getKey(), toAssociation( entry.getValue() ) );
		}

		return associations;
	}

	private NGAssociation toAssociation( NGBindingValue bindingValue ) {

		if( isInline() ) {
			try {
				return bindingValueForInlineBindingString( bindingValue.value() );
			}
			catch( NGHTMLFormatException e ) {
				throw new RuntimeException( e );
			}
		}
		else {
			return NGAssociationFactory.associationWithValue( bindingValue.value(), bindingValue.isQuoted() );
		}
	}

	public record NGBindingValue( boolean isQuoted, String value ) {}

	public static NGAssociation bindingValueForInlineBindingString( String value ) throws NGHTMLFormatException {
		Objects.requireNonNull( value );

		if( value.startsWith( "\"" ) ) {
			value = value.substring( 1 );

			if( value.endsWith( "\"" ) ) {
				value = value.substring( 0, value.length() - 1 );
			}
			else {
				throw new NGHTMLFormatException( value + " starts with quote but does not end with one." );
			}

			if( value.startsWith( "$" ) ) {
				value = value.substring( 1 );

				if( value.endsWith( "VALID" ) ) {
					value = value.replaceFirst( "\\s*//\\s*VALID", "" );
				}

				return NGAssociationFactory.associationWithValue( value, false );
			}
			else {
				value = value.replaceAll( "\\\\\\$", "\\$" );
				value = value.replaceAll( "\\\"", "\"" );
				return NGAssociationFactory.associationWithValue( value, true );
			}
		}

		return NGAssociationFactory.associationWithValue( value, false );
	}
}