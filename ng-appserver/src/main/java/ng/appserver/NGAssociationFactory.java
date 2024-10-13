package ng.appserver;

import java.util.Map;
import java.util.Objects;

public class NGAssociationFactory {

	public static NGAssociation constantValueAssociationWithValue( Object obj ) {
		return new NGConstantValueAssociation( obj );
	}

	public static NGAssociation associationWithKeyPath( String keyPath ) {

		if( keyPath.charAt( 0 ) == '^' ) {
			return new NGBindingAssociation( keyPath.substring( 1 ) );
		}

		return new NGKeyValueAssociation( keyPath );
	}

	public static NGAssociation associationWithValue( String associationValue, Map<String, String> quotedStrings ) {
		Objects.requireNonNull( associationValue );
		Objects.requireNonNull( quotedStrings );

		NGAssociation association = null;

		String quotedString = quotedStrings.get( associationValue );
		// MS: WO 5.4 converts \n to an actual newline. I don't know if WO 5.3 does, too, but let's go ahead and be compatible with them as long as nobody is yelling.
		if( quotedString != null ) {
			int backslashIndex = quotedString.indexOf( '\\' );
			if( backslashIndex != -1 ) {
				StringBuilder sb = new StringBuilder( quotedString );
				int length = sb.length();
				for( int i = backslashIndex; i < length; i++ ) {
					char ch = sb.charAt( i );
					if( ch == '\\' && i < length ) {
						char nextCh = sb.charAt( i + 1 );
						if( nextCh == 'n' ) {
							sb.replace( i, i + 2, "\n" );
						}
						else if( nextCh == 'r' ) {
							sb.replace( i, i + 2, "\r" );
						}
						else if( nextCh == 't' ) {
							sb.replace( i, i + 2, "\t" );
						}
						else {
							sb.replace( i, i + 2, String.valueOf( nextCh ) );
						}
						length--;
					}
				}
				quotedString = sb.toString();
			}
			association = NGAssociationFactory.constantValueAssociationWithValue( quotedString );
		}
		else if( isNumeric( associationValue ) ) {
			// CHECKME: This value conversion feels a little odd to perform here // Hugi 2023-07-01
			final Number number;

			if( associationValue != null && associationValue.contains( "." ) ) {
				number = Double.valueOf( associationValue );
			}
			else {
				number = Integer.parseInt( associationValue );
			}

			association = NGAssociationFactory.constantValueAssociationWithValue( number );
		}
		// CHECKME: I'm not a fan of interpreting strings as booleans // Hugi 2023-07-01
		else if( "true".equalsIgnoreCase( associationValue ) || "yes".equalsIgnoreCase( associationValue ) ) {
			association = NGConstantValueAssociation.TRUE;
		}
		else if( "false".equalsIgnoreCase( associationValue ) || "no".equalsIgnoreCase( associationValue ) || "nil".equalsIgnoreCase( associationValue ) || "null".equalsIgnoreCase( associationValue ) ) {
			association = NGConstantValueAssociation.FALSE;
		}
		else {
			association = NGAssociationFactory.associationWithKeyPath( associationValue );
		}

		return association;
	}

	static boolean isNumeric( String string ) {
		int length = string.length();

		if( length == 0 ) {
			return false;
		}

		boolean dot = false;
		int i = 0;
		char character = string.charAt( 0 );

		if( (character == '-') || (character == '+') ) {
			i = 1;
		}
		else if( character == '.' ) {
			i = 1;
			dot = true;
		}

		while( i < length ) {
			character = string.charAt( i++ );

			if( character == '.' ) {
				if( dot ) {
					return false;
				}
				dot = true;
			}
			else if( !(Character.isDigit( character )) ) {
				return false;
			}
		}

		return true;
	}
}