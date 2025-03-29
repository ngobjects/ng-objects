package ng.appserver.templating.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGHTMLParser {

	private static final Logger logger = LoggerFactory.getLogger( NGHTMLParser.class );

	private enum ParserState {
		Outside,
		InsideComment
	}

	private static final String JS_START_TAG = "<script";
	private static final String JS_END_TAG = "</script";
	private static final String WO_END_TAG = "</wo";
	private static final String WO_START_TAG = "<wo ";
	private static final String WEBOBJECT_END_TAG = "</webobject";
	private static final String WEBOBJECT_START_TAG = "<webobject";
	private static final String WO_COLON_END_TAG = "</wo:";
	private static final String WO_COLON_START_TAG = "<wo:";
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
		boolean flag = true; // Flag for what?
		ParserState parserState = ParserState.Outside;
		String token;

		if( _unparsedTemplate.startsWith( "<" ) || !templateTokenizer.hasMoreTokens() ) {
			token = null;
		}
		else {
			token = templateTokenizer.nextToken( "<" );
		}

		try {
			while( true ) {
				if( !templateTokenizer.hasMoreTokens() ) {
					break;
				}

				switch( parserState ) {
					case Outside:
						if( token != null ) {
							if( token.startsWith( ">" ) ) {
								token = token.substring( 1 );
							}
							_contentText.append( token );
						}
						token = templateTokenizer.nextToken( ">" );
						int tagIndex;

						// parses non wo: tags for dynamic bindings
						if( _parseStandardTags() ) {
							token = checkStandardTagForInlineBindings( token );
						}

						final String tagLowerCase = token.toLowerCase();

						if( tagLowerCase.startsWith( WEBOBJECT_START_TAG ) || tagLowerCase.startsWith( WO_COLON_START_TAG ) || tagLowerCase.startsWith( WO_START_TAG ) ) {
							if( token.endsWith( "/" ) ) {
								startOfWebObjectTag( token.substring( 0, token.length() - 1 ) );
								endOfWebObjectTag( "/" );
							}
							else {
								startOfWebObjectTag( token );
							}
						}
						else if( (tagIndex = tagLowerCase.indexOf( WEBOBJECT_START_TAG )) > 1 || (tagIndex = tagLowerCase.indexOf( WO_COLON_START_TAG )) > 1 || (tagIndex = tagLowerCase.indexOf( WO_START_TAG )) > 1 ) {
							_contentText.append( token.substring( 0, token.lastIndexOf( "<" ) ) );
							if( token.endsWith( "/" ) ) {
								startOfWebObjectTag( token.substring( tagIndex, token.length() - 1 ) );
								endOfWebObjectTag( "/" );
							}
							else {
								startOfWebObjectTag( token.substring( tagIndex, token.length() ) );
							}
						}
						else if( tagLowerCase.startsWith( WEBOBJECT_END_TAG ) || tagLowerCase.startsWith( WO_COLON_END_TAG ) || tagLowerCase.equals( WO_END_TAG ) ) {
							endOfWebObjectTag( token );
						}
						else if( tagLowerCase.startsWith( NGHTMLParser.JS_START_TAG ) ) {
							didParseText();
							_contentText.append( token );
							_contentText.append( '>' );
							flag = false;
						}
						else if( tagLowerCase.startsWith( NGHTMLParser.JS_END_TAG ) ) {
							didParseText();
							_contentText.append( token );
							_contentText.append( '>' );
							flag = true;
						}
						else if( token.startsWith( "<!--" ) && flag ) {
							didParseText();
							_contentText.append( token );
							if( token.endsWith( "--" ) ) {
								_contentText.append( '>' );
								didParseComment();
							}
							else {
								_contentText.append( '>' );
								parserState = ParserState.InsideComment;
							}
						}
						else {
							_contentText.append( token );
							_contentText.append( '>' );
						}
						break;

					case InsideComment:
						token = templateTokenizer.nextToken( ">" );
						_contentText.append( token );
						_contentText.append( '>' );
						if( token.endsWith( "--" ) ) {
							didParseComment();
							parserState = ParserState.Outside;
						}
						break;

					default:
						break;
				}
				token = null;
				if( parserState == ParserState.Outside ) {
					token = templateTokenizer.nextToken( "<" );
				}
			}
		}
		catch( NoSuchElementException e ) {
			logger.error( "No Such element dude", e );
			didParseText();
			return;
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

	private void startOfWebObjectTag( String token ) throws NGHTMLFormatException, NGDeclarationFormatException {
		didParseText();
		_contentText.append( token );
		didParseOpeningWebObjectTag();
	}

	private void endOfWebObjectTag( String token ) throws NGDeclarationFormatException, NGHTMLFormatException {
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

	private void didParseComment() {
		logger.debug( "Parsed Comment ({}) : {}", _contentText.length(), _contentText );

		if( _contentText.length() > 0 ) {
			_parserDelegate.didParseComment( _contentText.toString() );
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
	 * Indicates that we want to parse the attributes of "standard tags", i.e. non dynamic tags for dynamic bindings
	 *
	 * CHECKME: This isn't really a feature I want to support, but I'm keeping it around for a little while // Hugi 2022-10-13
	 */
	private static final boolean _parseStandardTags() {
		return false;
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

		if( tokenLowerCase.startsWith( WEBOBJECT_START_TAG ) || tokenLowerCase.startsWith( WO_COLON_START_TAG ) || tokenLowerCase.startsWith( WO_START_TAG ) || tokenLowerCase.startsWith( XML_CDATA_START_TAG ) ) {
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