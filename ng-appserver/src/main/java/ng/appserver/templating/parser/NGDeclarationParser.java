package ng.appserver.templating.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import ng.appserver.templating.parser.NGDeclaration.NGBindingValue;

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

			final Map<String, NGBindingValue> bindings = _bindingsForDictionaryString( declarationHeader, declarationBody );
			final NGDeclaration declaration = new NGDeclaration( false, tagName, type, bindings );
			declarations.put( tagName, declaration );
		}

		return declarations;
	}

	private Map<String, NGBindingValue> _bindingsForDictionaryString( String declarationHeader, String declarationBody ) throws NGDeclarationFormatException {
		Objects.requireNonNull( declarationHeader );
		Objects.requireNonNull( declarationBody );

		final Map<String, NGBindingValue> bindings = new HashMap<>();
		String trimmedDeclarationBody = declarationBody.trim();

		if( !trimmedDeclarationBody.startsWith( "{" ) && !trimmedDeclarationBody.endsWith( "}" ) ) {
			throw new NGDeclarationFormatException( "Internal inconsistency : invalid dictionary for declaration:\n" + declarationHeader + " " + declarationBody );
		}

		int declarationBodyLength = trimmedDeclarationBody.length();

		if( declarationBodyLength <= 2 ) {
			return bindings;
		}

		trimmedDeclarationBody = trimmedDeclarationBody.substring( 1, declarationBodyLength - 1 ).trim();

		final String[] bindingStrings = trimmedDeclarationBody.split( ";" );

		for( String binding : bindingStrings ) {
			binding = binding.trim();

			if( binding.length() != 0 ) {
				final int equalsIndex = binding.indexOf( '=' );

				if( equalsIndex < 0 ) {
					throw new NGDeclarationFormatException( "Invalid line. No equal in line:\n" + binding + "\nfor declaration:\n" + declarationHeader + " " + declarationBody );
				}

				String key = binding.substring( 0, equalsIndex ).trim();

				if( key.length() == 0 ) {
					throw new NGDeclarationFormatException( "Missing binding in line:\n" + binding + "\nfor declaration:\n" + declarationHeader + " " + declarationBody );
				}

				String value = binding.substring( equalsIndex + 1 ).trim();

				if( value.length() == 0 ) {
					throw new NGDeclarationFormatException( "Missing value in line:\n" + binding + "\nfor declaration:\n" + declarationHeader + " " + declarationBody );
				}

				final String quotedKey = _quotedStrings.get( key );
				boolean keyIsQuoted = quotedKey != null;

				if( keyIsQuoted ) {
					key = quotedKey;
				}

				final String quotedValue = _quotedStrings.get( value );
				boolean valueIsQuoted = quotedValue != null;

				if( valueIsQuoted ) {
					value = quotedValue;
				}

				bindings.put( key, new NGBindingValue( valueIsQuoted, value ) );
			}
		}

		return bindings;
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