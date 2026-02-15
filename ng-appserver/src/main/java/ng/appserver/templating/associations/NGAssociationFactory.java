package ng.appserver.templating.associations;

import java.util.Objects;

import ng.appserver.templating.parser.NGDeclaration.NGBindingValue;

public class NGAssociationFactory {

	private static final NGConstantValueAssociation TRUE = new NGConstantValueAssociation( Boolean.TRUE );
	private static final NGConstantValueAssociation FALSE = new NGConstantValueAssociation( Boolean.FALSE );

	/**
	 * @return An association for the given binding value
	 */
	public static NGAssociation associationForBindingValue( final NGBindingValue bindingValue, final boolean isInline ) {

		return switch( bindingValue ) {
			case NGBindingValue.BooleanPresence b -> TRUE;
			case NGBindingValue.Value v -> isInline ? associationForInlineBindingValue( v.value() ) : associationForWodBindingValue( v.value(), v.isQuoted() );
		};
	}

	/**
	 * @return An association for the given inline binding value
	 */
	private static NGAssociation associationForInlineBindingValue( String value ) {
		Objects.requireNonNull( value );

		if( value.startsWith( "\"" ) ) {
			value = value.substring( 1 );

			if( value.endsWith( "\"" ) ) {
				value = value.substring( 0, value.length() - 1 );
			}
			else {
				throw new IllegalArgumentException( value + " starts with quote but does not end with one. The parser should have already failed on this" );
			}

			if( value.startsWith( "$" ) ) {
				value = value.substring( 1 );

				if( value.endsWith( "VALID" ) ) {
					value = value.replaceFirst( "\\s*//\\s*VALID", "" );
				}
			}
			else {
				value = value.replace( "\\$", "$" ); // Unescape escaped dollar signs
				value = value.replace( "\\\"", "\"" ); // Unescape escaped quotes
				return associationForConstantStringValue( value );
			}
		}

		return associationForDynamicValue( value, true );
	}

	/**
	 * @return An association for the given wod binding value
	 */
	private static NGAssociation associationForWodBindingValue( final String associationValue, final boolean isQuoted ) {

		if( isQuoted ) {
			return associationForConstantStringValue( associationValue );
		}

		return associationForDynamicValue( associationValue, false );
	}

	/**
	 * @return An association for the given constant string value, by applying the necessary escapes
	 */
	private static NGAssociation associationForConstantStringValue( String associationValue ) {
		associationValue = applyEscapes( associationValue );
		return associationForConstantValue( associationValue );
	}

	/**
	 * @return And association for the given dynamic value (a "dynamic value" being what we're calling any value following a $ in an inline binding or an unquoted value in a wod binding)
	 *
	 * CHECKME:
	 * We still need a nicer way to differentiate between inline/wod associations values.
	 * This parameter is currently only used to decide how we interpret boolean values (exactly $true and $false in inline bindings, a bunch of case insensitive values for WODs)
	 * // Hugi 2025-03-26
	 */
	private static NGAssociation associationForDynamicValue( final String associationValue, final boolean isInline ) {

		if( isNumeric( associationValue ) ) {
			final Number number = numericValueFromString( associationValue );
			return associationForConstantValue( number );
		}

		if( isInline ) {
			if( "true".equals( associationValue ) ) {
				return TRUE;
			}

			if( "false".equals( associationValue ) ) {
				return FALSE;
			}
		}
		else {
			if( "true".equalsIgnoreCase( associationValue ) || "yes".equalsIgnoreCase( associationValue ) ) {
				return TRUE;
			}

			if( "false".equalsIgnoreCase( associationValue ) || "no".equalsIgnoreCase( associationValue ) || "nil".equalsIgnoreCase( associationValue ) || "null".equalsIgnoreCase( associationValue ) ) {
				return FALSE;
			}
		}

		return associationForKeyPath( associationValue );
	}

	/**
	 * @return An association that returns the given value
	 */
	private static NGAssociation associationForConstantValue( final Object value ) {
		return new NGConstantValueAssociation( value );
	}

	/**
	 * @return An association for resolving the given keyPath
	 */
	private static NGAssociation associationForKeyPath( final String keyPath ) {

		if( keyPath.charAt( 0 ) == '^' ) {
			return new NGBindingAssociation( keyPath.substring( 1 ) );
		}

		return new NGKeyValueAssociation( keyPath );
	}

	/**
	 * @return The given string with escape sequences \r, \n and \t converted to what they represent
	 */
	static String applyEscapes( String string ) {

		int firstBackslashIndex = string.indexOf( '\\' );

		if( firstBackslashIndex == -1 ) {
			return string;
		}

		final StringBuilder sb = new StringBuilder( string );

		for( int i = firstBackslashIndex; i < sb.length(); i++ ) {
			if( sb.charAt( i ) == '\\' && i + 1 < sb.length() ) {
				char nextChar = sb.charAt( i + 1 );

				switch( nextChar ) {
					case 'n' -> sb.replace( i, i + 2, "\n" );
					case 'r' -> sb.replace( i, i + 2, "\r" );
					case 't' -> sb.replace( i, i + 2, "\t" );
					case '\\' -> sb.replace( i, i + 2, "\\" );
					default -> throw new IllegalArgumentException( "Unknown escape character: '%s' (%s) ".formatted( nextChar, Character.getName( nextChar ) ) );
				}
			}
		}

		return sb.toString();
	}

	/**
	 * @return The given string converted to a number. If the number contains a decimal separator (period), returns a Double, if no decimal separator, returns an Integer.
	 */
	static Number numericValueFromString( final String numericString ) {

		if( numericString.contains( "." ) ) {
			return Double.valueOf( numericString );
		}

		// CHECKME: Determine the number's size and return a Long if it doesn't fit in an int? Or just always return a Long? // Hugi 2025-03-19
		return Integer.valueOf( numericString );
	}

	/**
	 * @return true if this is a numeric string. Note that a signed number (i.e. prefixed with a plus or a minus) is considered numeric
	 */
	static boolean isNumeric( final String string ) {

		int length = string.length();

		if( length == 0 ) {
			return false;
		}

		boolean dotAlreadySpotted = false;

		int i = 0;
		char character = string.charAt( i );

		if( (character == '-') || (character == '+') ) {
			i = 1;
		}
		else if( character == '.' ) {
			i = 1;
			dotAlreadySpotted = true;
		}

		// If we've already advanced and the string's length is only 1, the string is merely a period or a sign and not numeric.
		if( i == 1 && length == 1 ) {
			return false;
		}

		while( i < length ) {
			character = string.charAt( i++ );

			if( character == '.' ) {
				if( dotAlreadySpotted ) {
					return false;
				}
				dotAlreadySpotted = true;
			}
			else if( !(Character.isDigit( character )) ) {
				return false;
			}
		}

		return true;
	}
}