package ng.appserver.templating;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import ng.appserver.NGAssociation;
import ng.appserver.NGAssociationFactory;
import ng.appserver.NGConstantValueAssociation;

public class NGDeclarationParser {

	private enum ParserState {
		Outside,
		InsideComment
	}

	private static final String ESCAPED_QUOTE_STRING = "_WO_ESCAPED_QUOTE_";
	private static final String QUOTED_STRING_KEY = "_WODP_";

	/**
	 * CHECKME: Keeping this an instance variable feels a little odd. Might want to revisit this design // Hugi 2023-07-01
	 */
	private final Map<String, String> _quotedStrings = new HashMap<>();

	public static Map<String, NGDeclaration> declarationsWithString( String declarationStr ) throws NGDeclarationFormatException {
		final NGDeclarationParser declarationParser = new NGDeclarationParser();
		return declarationParser.parseDeclarations( declarationStr );
	}

	private Map<String, NGDeclaration> parseDeclarations( String declarationStr ) throws NGDeclarationFormatException {
		String strWithoutComments = _removeOldStyleCommentsFromString( declarationStr );
		strWithoutComments = _removeNewStyleCommentsAndQuotedStringsFromString( strWithoutComments );
		return parseDeclarationsWithoutComments( strWithoutComments );
	}

	/**
	 * Strips old style comments from the declaration string.
	 * "Old style comments" start with slash-asterisk and end with asterisk-slash.
	 *
	 * Should be private, only friendly due to testing
	 *
	 * CHECKME: Shouldn't we fail on an unclosed comment?
	 */
	static String _removeOldStyleCommentsFromString( String str ) {
		Objects.requireNonNull( str );

		final StringBuilder stringb = new StringBuilder( 100 );
		final StringBuilder stringb1 = new StringBuilder( 100 );
		final StringTokenizer tokenizer = new StringTokenizer( str, "/", true );

		ParserState state = ParserState.Outside;

		do {
			if( !tokenizer.hasMoreTokens() ) {
				break;
			}
			String token = tokenizer.nextToken();
			switch( state ) {
			case Outside:
				if( token.equals( "/" ) ) {
					token = tokenizer.nextToken();
					if( token.startsWith( "*" ) ) {
						state = ParserState.InsideComment;
						stringb1.append( '/' );
						stringb1.append( token );
					}
					else {
						stringb.append( '/' );
						stringb.append( token );
					}
				}
				else {
					stringb.append( token );
				}
				break;

			case InsideComment:
				stringb1.append( token );
				String s2 = stringb1.toString();
				if( s2.endsWith( "*/" ) && !s2.equals( "/*/" ) ) {
					state = ParserState.Outside;
				}
				break;
			}
		}
		while( true );

		return stringb.toString();
	}

	private String _removeNewStyleCommentsAndQuotedStringsFromString( String declarationsStr ) {
		Objects.requireNonNull( declarationsStr );

		final String escapedQuoteStr = declarationsStr.replace( "\\\"", NGDeclarationParser.ESCAPED_QUOTE_STRING );
		final StringBuilder declarationWithoutCommentsBuffer = new StringBuilder( 100 );
		final StringTokenizer tokenizer = new StringTokenizer( escapedQuoteStr, "/\"", true );

		while( tokenizer.hasMoreTokens() ) {
			String token = tokenizer.nextToken( "/\"" );
			if( token.equals( "/" ) ) {
				token = tokenizer.nextToken( "\n" );
				if( token.startsWith( "/" ) ) {
					token = token.replace( NGDeclarationParser.ESCAPED_QUOTE_STRING, "\\\"" );
					declarationWithoutCommentsBuffer.append( '\n' );
					tokenizer.nextToken();
				}
				else {
					declarationWithoutCommentsBuffer.append( '/' );
					declarationWithoutCommentsBuffer.append( token );
				}
			}
			else if( token.equals( "\"" ) ) {
				token = tokenizer.nextToken( "\"" );
				if( token.equals( "\"" ) ) {
					token = "";
				}
				else {
					tokenizer.nextToken();
				}
				String quotedStringKey = NGDeclarationParser.QUOTED_STRING_KEY + _quotedStrings.size();
				token = token.replace( NGDeclarationParser.ESCAPED_QUOTE_STRING, "\"" );
				_quotedStrings.put( quotedStringKey, token );
				declarationWithoutCommentsBuffer.append( quotedStringKey );
			}
			else {
				declarationWithoutCommentsBuffer.append( token );
			}
		}

		return declarationWithoutCommentsBuffer.toString();
	}

	private Map<String, NGDeclaration> parseDeclarationsWithoutComments( String declarationWithoutComment ) throws NGDeclarationFormatException {
		Objects.requireNonNull( declarationWithoutComment );

		final Map<String, NGDeclaration> declarations = new HashMap<>();
		final Map<String, String> rawDeclarations = _rawDeclarationsWithoutComment( declarationWithoutComment );

		for( String declarationHeader : rawDeclarations.keySet() ) {
			final String declarationBody = rawDeclarations.get( declarationHeader );
			final int colonIndex = declarationHeader.indexOf( ':' );

			if( colonIndex < 0 ) {
				throw new NGDeclarationFormatException( "Missing ':' for declaration:\n" + declarationHeader + " " + declarationBody );
			}

			String tagName = declarationHeader.substring( 0, colonIndex ).trim();

			if( tagName.length() == 0 ) {
				throw new NGDeclarationFormatException( "Missing tag name for declaration:\n" + declarationHeader + " " + declarationBody );
			}

			if( declarations.get( tagName ) != null ) {
				throw new NGDeclarationFormatException( "Duplicate tag name '" + tagName + "' in declaration:\n" + declarationBody );
			}

			String type = declarationHeader.substring( colonIndex + 1 ).trim();

			if( type.length() == 0 ) {
				throw new NGDeclarationFormatException( "Missing element name for declaration:\n" + declarationHeader + " " + declarationBody );
			}

			final Map<String, NGAssociation> associations = _associationsForDictionaryString( declarationHeader, declarationBody );
			final NGDeclaration declaration = new NGDeclaration( tagName, type, associations );
			declarations.put( tagName, declaration );
		}

		return declarations;
	}

	private Map<String, NGAssociation> _associationsForDictionaryString( String declarationHeader, String declarationBody ) throws NGDeclarationFormatException {
		Objects.requireNonNull( declarationHeader );
		Objects.requireNonNull( declarationBody );

		final Map<String, NGAssociation> associations = new HashMap<>();
		String trimmedDeclarationBody = declarationBody.trim();

		if( !trimmedDeclarationBody.startsWith( "{" ) && !trimmedDeclarationBody.endsWith( "}" ) ) {
			throw new NGDeclarationFormatException( "Internal inconsistency : invalid dictionary for declaration:\n" + declarationHeader + " " + declarationBody );
		}

		int declarationBodyLength = trimmedDeclarationBody.length();

		if( declarationBodyLength <= 2 ) {
			return associations;
		}

		trimmedDeclarationBody = trimmedDeclarationBody.substring( 1, declarationBodyLength - 1 ).trim();

		final String[] bindings = trimmedDeclarationBody.split( ";" );

		for( String binding : bindings ) {
			binding = binding.trim();

			if( binding.length() != 0 ) {
				final int equalsIndex = binding.indexOf( '=' );

				if( equalsIndex < 0 ) {
					throw new NGDeclarationFormatException( "Invalid line. No equal in line:\n" + binding + "\nfor declaration:\n" + declarationHeader + " " + declarationBody );
				}

				final String key = binding.substring( 0, equalsIndex ).trim();

				if( key.length() == 0 ) {
					throw new NGDeclarationFormatException( "Missing binding in line:\n" + binding + "\nfor declaration:\n" + declarationHeader + " " + declarationBody );
				}

				final String value = binding.substring( equalsIndex + 1 ).trim();

				if( value.length() == 0 ) {
					throw new NGDeclarationFormatException( "Missing value in line:\n" + binding + "\nfor declaration:\n" + declarationHeader + " " + declarationBody );
				}

				final NGAssociation association = NGDeclarationParser._associationWithKey( value, _quotedStrings );
				final String quotedString = _quotedStrings.get( key );

				if( quotedString != null ) {
					associations.put( quotedString, association );
				}
				else {
					associations.put( key, association );
				}
			}
		}

		return associations;
	}

	/**
	 * CHECKME: Doesn't this belong in NGAssociationFactory? // Hugi 2022-04-27
	 */
	public static NGAssociation _associationWithKey( String associationValue, Map<String, String> quotedStrings ) {
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
			association = NGAssociationFactory.associationWithValue( quotedString );
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

			association = NGAssociationFactory.associationWithValue( number );
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

	private static Map<String, String> _rawDeclarationsWithoutComment( String declarationStr ) {
		final Map<String, String> declarations = new HashMap<>();
		final StringBuilder declarationWithoutCommentBuffer = new StringBuilder( 100 );
		final StringTokenizer tokenizer = new StringTokenizer( declarationStr, "{", true );

		while( tokenizer.hasMoreTokens() ) {
			String token = tokenizer.nextToken( "{" );

			if( token.equals( "{" ) ) {
				token = tokenizer.nextToken( "}" );

				if( token.equals( "}" ) ) {
					token = "";
				}
				else {
					tokenizer.nextToken();
				}

				String declarationWithoutComment = declarationWithoutCommentBuffer.toString();

				if( declarationWithoutComment.startsWith( ";" ) ) {
					declarationWithoutComment = declarationWithoutComment.substring( 1 );
				}

				declarations.put( declarationWithoutComment.trim(), "{" + token + "}" );
				declarationWithoutCommentBuffer.setLength( 0 );
			}
			else {
				declarationWithoutCommentBuffer.append( token );
			}
		}

		return declarations;
	}

	@Override
	public String toString() {
		return "quotedStrings = " + _quotedStrings.toString() + ">";
	}
}