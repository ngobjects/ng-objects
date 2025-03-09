package ng.appserver;

import java.util.Objects;

import ng.appserver.templating.parser.NGDeclaration.NGBindingValue;

public class NGAssociationFactory {

	private static final NGConstantValueAssociation TRUE = new NGConstantValueAssociation( Boolean.TRUE );
	private static final NGConstantValueAssociation FALSE = new NGConstantValueAssociation( Boolean.FALSE );

	/**
	 * @return An association that returns the given value
	 */
	public static NGAssociation constantValueAssociationWithValue( final Object value ) {
		return new NGConstantValueAssociation( value );
	}

	/**
	 * @return An association for resolving the given keyPath
	 */
	public static NGAssociation associationWithKeyPath( final String keyPath ) {

		if( keyPath.charAt( 0 ) == '^' ) {
			return new NGBindingAssociation( keyPath.substring( 1 ) );
		}

		return new NGKeyValueAssociation( keyPath );
	}

	/**
	 * @return An association for the given binding value
	 */
	public static NGAssociation associationForBindingValue( final NGBindingValue bindingValue, final boolean isInline ) {

		if( isInline ) {
			return associationForInlineBindingValue( bindingValue.value() );
		}

		return associationForWodBindingValue( bindingValue.value(), bindingValue.isQuoted() );
	}

	/**
	 * @return An association for the given inline binding value
	 *
	 * FIXME: We shouldn't be invoking "assicationForWodBindingValue" from here. Handing inline/wod assiciations values totally separately would be much cleaner // Hugi 2025-03-09
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

				return associationForWodBindingValue( value, false );
			}
			else {
				// FIXME: Figure out what the absolute ding-diddly we're doing here // Hugi 2024-11-23
				value = value.replaceAll( "\\\\\\$", "\\$" );
				value = value.replaceAll( "\\\"", "\"" );
				return associationForWodBindingValue( value, true );
			}
		}

		return associationForWodBindingValue( value, false );
	}

	/**
	 * @return An association for the given wod binding value
	 */
	private static NGAssociation associationForWodBindingValue( String associationValue, final boolean isQuoted ) {
		Objects.requireNonNull( associationValue );

		if( isQuoted ) {
			associationValue = applyEscapes( associationValue );
			return constantValueAssociationWithValue( associationValue );
		}

		if( isNumeric( associationValue ) ) {
			final Number number = numericValueFromString( associationValue );
			return constantValueAssociationWithValue( number );
		}

		if( "true".equalsIgnoreCase( associationValue ) || "yes".equalsIgnoreCase( associationValue ) ) {
			return TRUE;
		}

		if( "false".equalsIgnoreCase( associationValue ) || "no".equalsIgnoreCase( associationValue ) || "nil".equalsIgnoreCase( associationValue ) || "null".equalsIgnoreCase( associationValue ) ) {
			return FALSE;
		}

		return associationWithKeyPath( associationValue );
	}

	/**
	 * @return The given string with escape sequences \r, \n and \t converted to what they represent
	 */
	private static String applyEscapes( String string ) {
		int firstBackslashIndex = string.indexOf( '\\' );

		if( firstBackslashIndex != -1 ) {
			final StringBuilder sb = new StringBuilder( string );

			for( int i = firstBackslashIndex; i < sb.length(); i++ ) {
				if( sb.charAt( i ) == '\\' && i + 1 < sb.length() ) {
					char nextChar = sb.charAt( i + 1 );

					switch( nextChar ) {
						case 'n' -> sb.replace( i, i + 2, "\n" );
						case 'r' -> sb.replace( i, i + 2, "\r" );
						case 't' -> sb.replace( i, i + 2, "\t" );
						default -> sb.deleteCharAt( i ); // Remove the backslash if not followed by a known escape. FIXME: We probably want to throw an exception (or just keep in the backslash) if an unknown escape character is encountered // Hugi 2025-03-09
					}
				}
			}

			string = sb.toString();
		}

		return string;
	}

	/**
	 * @return The given string converted to a number. If the number contains a decimal separator (period), returns a Double, if no decimal separator, returns an Integer.
	 */
	static Number numericValueFromString( final String string ) {
		final Number number;

		if( string.contains( "." ) ) {
			number = Double.valueOf( string );
		}
		else {
			number = Integer.valueOf( string ); // CHEKME: Determine the number's size and return a Long if it doesn't fit in an int?
		}

		return number;
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