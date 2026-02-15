package ng.appserver.templating.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGHTMLParser {

	private static final Logger logger = LoggerFactory.getLogger( NGHTMLParser.class );

	private static final String WO_END_TAG = "</wo";
	private static final String WO_START_TAG = "<wo ";
	private static final String WEBOBJECT_END_TAG = "</webobject";
	private static final String WEBOBJECT_START_TAG = "<webobject";
	private static final String XML_CDATA_START_TAG = "<![CDATA[";

	/**
	 * This is only used for tags that are "dynamified" when _parseStandardTags is set to true.
	 * This value will get prepended to the tag, NGTemplateParser will then look for it and use it as a hint that it needs to crete an NGGenericContainer element for it with it's bindings.
	 */
	public static final String WO_REPLACEMENT_MARKER = "__REPL__";

	/**
	 * Only used by the "dynamification of standard tags" feature
	 */
	private Map<String, Stack<String>> _stackDict;

	private final NGTemplateParser _parserDelegate;
	private final String _unparsedTemplate;
	private final StringBuilder _contentText;

	public NGHTMLParser( final NGTemplateParser parserDelegate, final String unparsedTemplate ) {
		Objects.requireNonNull( parserDelegate );
		Objects.requireNonNull( unparsedTemplate );
		_parserDelegate = parserDelegate;
		_unparsedTemplate = unparsedTemplate;
		_contentText = new StringBuilder( 128 );
	}

	public void parseHTML() throws NGHTMLFormatException, NGDeclarationFormatException {
		_stackDict = new HashMap<>();

		final NGStringTokenizer templateTokenizer = new NGStringTokenizer( _unparsedTemplate, "<" );
		String token;

		if( _unparsedTemplate.startsWith( "<" ) || !templateTokenizer.hasMoreTokens() ) {
			token = null;
		}
		else {
			token = templateTokenizer.nextToken( "<" );
		}

		while( templateTokenizer.hasMoreTokens() ) {
			if( token != null ) {
				if( token.startsWith( ">" ) ) {
					token = token.substring( 1 );
				}
				_contentText.append( token );
			}
			token = templateTokenizer.nextToken( ">" );
			int tagIndex;

			final String tagLowerCase = token.toLowerCase();

			if( isNamespacedStartTag( tagLowerCase ) || tagLowerCase.startsWith( WEBOBJECT_START_TAG ) || tagLowerCase.startsWith( WO_START_TAG ) ) {
				if( token.endsWith( "/" ) ) {
					startOfWebObjectTag( token.substring( 0, token.length() - 1 ) );
					endOfWebObjectTag( "/" );
				}
				else {
					startOfWebObjectTag( token );
				}
			}
			else if( (tagIndex = indexOfNamespacedStartTag( tagLowerCase )) > 1 || (tagIndex = tagLowerCase.indexOf( WEBOBJECT_START_TAG )) > 1 || (tagIndex = tagLowerCase.indexOf( WO_START_TAG )) > 1 ) {
				// Used if you have a comment block that contains a dynamic tag, an example being <!-- <wo:str value="$someMessage" /> -->
				_contentText.append( token.substring( 0, token.lastIndexOf( "<" ) ) );
				if( token.endsWith( "/" ) ) {
					startOfWebObjectTag( token.substring( tagIndex, token.length() - 1 ) );
					endOfWebObjectTag( "/" );
				}
				else {
					startOfWebObjectTag( token.substring( tagIndex, token.length() ) );
				}
			}
			else if( isNamespacedEndTag( tagLowerCase ) || tagLowerCase.startsWith( WEBOBJECT_END_TAG ) || tagLowerCase.equals( WO_END_TAG ) ) {
				endOfWebObjectTag( token );
			}
			else {
				_contentText.append( token );
				_contentText.append( '>' );
			}

			token = templateTokenizer.nextToken( "<" );
		}

		if( token != null ) {
			if( token.startsWith( ">" ) ) {
				token = token.substring( 1 );
			}
			_contentText.append( token );
		}

		didParseText();

		_stackDict = null;
	}

	/**
	 * @return true if the given lowercase token starts with a namespaced opening tag pattern, e.g. "<wo:", "<ui:", "<ng:", etc.
	 *
	 * Matches the pattern: <identifier:identifier
	 * Note: the token includes the leading '<' character from the template.
	 */
	private static boolean isNamespacedStartTag( final String tagLowerCase ) {

		if( !tagLowerCase.startsWith( "<" ) ) {
			return false;
		}

		final int colonIndex = tagLowerCase.indexOf( ':' );

		if( colonIndex < 2 ) { // Must have at least "<" + one letter before ":"
			return false;
		}

		// Verify everything between '<' and ':' is a letter (the namespace)
		for( int i = 1; i < colonIndex; i++ ) {
			if( !Character.isLetter( tagLowerCase.charAt( i ) ) ) {
				return false;
			}
		}

		// Must have something after the colon (the element type)
		return colonIndex < tagLowerCase.length() - 1;
	}

	/**
	 * @return true if the given lowercase token is a namespaced end tag, e.g. "</wo:", "</ui:", etc.
	 *
	 * Note: the token includes the leading '<', so end tags look like "</wo:Type"
	 */
	private static boolean isNamespacedEndTag( final String tagLowerCase ) {

		if( !tagLowerCase.startsWith( "</" ) ) {
			return false;
		}

		// Re-check as if it were a start tag by replacing "</" with "<"
		return isNamespacedStartTag( "<" + tagLowerCase.substring( 2 ) );
	}

	/**
	 * @return The index of a namespaced start tag within the given string, or -1 if not found
	 *
	 * Searches for patterns like "<wo:", "<ui:", "<ng:", etc. embedded within the string.
	 * Returns the index of the '<' character of the embedded match.
	 *
	 * Note: This is used for cases like "<!-- <wo:str .../> -->" where a dynamic tag is embedded within other content.
	 */
	private static int indexOfNamespacedStartTag( final String tagLowerCase ) {
		int searchFrom = 0;

		while( searchFrom < tagLowerCase.length() ) {
			final int ltIndex = tagLowerCase.indexOf( '<', searchFrom );

			if( ltIndex == -1 ) {
				return -1;
			}

			if( isNamespacedStartTag( tagLowerCase.substring( ltIndex ) ) ) {
				return ltIndex;
			}

			searchFrom = ltIndex + 1;
		}

		return -1;
	}

	private void startOfWebObjectTag( String token ) throws NGHTMLFormatException, NGDeclarationFormatException {
		didParseText();
		_contentText.append( token );
		didParseOpeningWebObjectTag();
	}

	private void endOfWebObjectTag( String token ) throws NGHTMLFormatException {
		didParseText();
		_contentText.append( token );
		didParseClosingWebObjectTag();
	}

	private void didParseText() {
		logger.debug( "Parsed Text ({}) : {}", _contentText.length(), _contentText );

		if( _contentText.length() > 0 ) {
			_parserDelegate.didParseText( _contentText.toString() );
			_contentText.setLength( 0 );
		}
	}

	private void didParseOpeningWebObjectTag() throws NGHTMLFormatException, NGDeclarationFormatException {
		logger.debug( "Parsed Opening WebObject ({}) : {}", _contentText.length(), _contentText );

		if( _contentText.length() > 0 ) {
			_parserDelegate.didParseOpeningWebObjectTag( _contentText.toString() );
			_contentText.setLength( 0 );
		}
	}

	private void didParseClosingWebObjectTag() throws NGHTMLFormatException {
		logger.debug( "Parsed Closing WebObject ({}) : {}", _contentText.length(), _contentText );

		if( _contentText.length() > 0 ) {
			_parserDelegate.didParseClosingWebObjectTag( _contentText.toString() );
			_contentText.setLength( 0 );
		}
	}

	/**
	 * Given a position in the template, tells us what line number it is.
	 *
	 * Method is currently not used, but I'm leaving the method in for future use, since I really want this functionality // Hugi 2022-07-29
	 */
	private int lineNumber( int indexInTemplate ) {
		String[] parts = _unparsedTemplate.split( "\n" );
		int currentLineIndex = 0;
		int currentPosition = 0;

		for( final String part : parts ) {
			currentLineIndex++;

			currentPosition = currentPosition + part.length() + 1;

			if( currentPosition >= indexInTemplate ) {
				logger.debug( currentPosition + " : " + indexInTemplate + " : " + currentLineIndex + " : " + part );
				return currentLineIndex;
			}
		}

		throw new IllegalStateException( "Went beyond the template length. This should never happen. " );
	}

	/**
	 * This method is invoked to parse dynamic bindings in standard tags.
	 *
	 * CHECKME: In the original Wonder version, this would catch any exception that happened and return the original token.
	 * I think we should rather try to fix the exceptions as they occur (that is, if we decide to actually support this feature)
	 *
	 * @return A rewritten token if it has an inline binding or a closing tag, if it belongs to a rewritten token
	 */
	private String checkStandardTagForInlineBindings( String token ) {

		if( token == null ) {
			return token;
		}

		final String tokenLowerCase = token.toLowerCase();

		if( tokenLowerCase.startsWith( WEBOBJECT_START_TAG ) || isNamespacedStartTag( tokenLowerCase ) || tokenLowerCase.startsWith( WO_START_TAG ) || tokenLowerCase.startsWith( XML_CDATA_START_TAG ) ) {
			// we return immediately, if it is a webobject token or CDATA tag
			return token;
		}

		String[] tokenParts = token.split( " " );
		String tokenPart = tokenParts[0].substring( 1 );

		if( (token.indexOf( "\"$" ) != -1 || token.indexOf( "\"~" ) != -1) && token.startsWith( "<" ) ) {
			// we assume a dynamic tag
			token = token.replaceAll( tokenParts[0], "<wo:" + WO_REPLACEMENT_MARKER + tokenPart );
			if( logger.isDebugEnabled() ) {
				logger.debug( "Rewritten <" + tokenPart + " ...> tag to <wo:" + tokenPart + " ...>" );
			}

			if( !token.endsWith( "/" ) ) {
				// no need to keep information for self closing tags
				Stack<String> stack = _stackDict.get( tokenPart );

				if( stack == null ) {
					// create one and push a marker
					stack = new Stack<>();
					stack.push( WO_REPLACEMENT_MARKER );
					_stackDict.put( tokenPart, stack );
				}
				else {
					// just push a marker
					stack.push( WO_REPLACEMENT_MARKER );
					_stackDict.put( tokenPart, stack );
				}
			}
		}
		else if( !token.startsWith( "</" ) && _stackDict.containsKey( tokenPart ) ) {
			// standard opening tag
			final Stack<String> stack = _stackDict.get( tokenPart );

			if( stack != null ) {
				stack.push( tokenPart );
				_stackDict.put( tokenPart, stack );
			}
		}
		else if( token.startsWith( "</" ) ) {
			// closing tag
			final Stack<String> stack = _stackDict.get( tokenParts[0].substring( 2 ) );

			if( stack != null && !stack.empty() ) {
				final String stackContent = stack.pop();

				if( stackContent.equals( WO_REPLACEMENT_MARKER ) ) {
					if( logger.isDebugEnabled() ) {
						logger.debug( "Replaced end tag for '" + tokenParts[0].substring( 2 ) + "' with 'wo' endtag" );
					}
					token = "</wo";
				}
			}
		}

		return token;
	}
}
