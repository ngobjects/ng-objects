package ng.appserver.templating.parser.legacy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import ng.appserver.templating.parser.NGDeclaration;
import ng.appserver.templating.parser.NGDeclaration.NGBindingValue;
import ng.appserver.templating.parser.NGDeclarationFormatException;
import ng.appserver.templating.parser.NGDeclarationParser;
import ng.appserver.templating.parser.NGHTMLFormatException;
import ng.appserver.templating.parser.model.PBasicNode;
import ng.appserver.templating.parser.model.PCommentNode;
import ng.appserver.templating.parser.model.PNode;
import ng.appserver.templating.parser.model.PRawNode;
import ng.appserver.templating.parser.model.PRootNode;
import ng.appserver.templating.parser.model.SourceRange;

/**
 * The primary entry point for component parsing
 */

public class NGTemplateParser {

	/**
	 * The namespace reserved for parser directives
	 */
	private static final String PARSER_NAMESPACE = "p";

	/**
	 * Parser directive: raw/verbatim block (content not processed, included in output)
	 */
	private static final String DIRECTIVE_RAW = "raw";

	/**
	 * Parser directive: developer comment (content not processed, stripped from output)
	 */
	private static final String DIRECTIVE_COMMENT = "comment";

	/**
	 * The default namespace used for wod-style declarations and the legacy "wo" prefix
	 */
	private static final String DEFAULT_NAMESPACE = "wo";

	/**
	 * Our context, i.e. the dynamic tag currently being parsed
	 */
	private NGDynamicHTMLTag _currentDynamicTag = new NGDynamicHTMLTag();

	/**
	 * The declarations parsed from the provided wod string (if present)
	 */
	private Map<String, NGDeclaration> _declarations;

	/**
	 * Keeps track of the number of parsed inline tags. Used to generate a declaration name for the tag
	 *
	 * CHECKME: We don't really need to keep track of the number of parsed inline tags anymore // Hugi 2024-11-16
	 */
	@Deprecated
	private int _inlineTagCount;

	/**
	 * The template's HTML string
	 */
	private final String _htmlString;

	/**
	 * The template's declaration string (a.k.a. the wod file)
	 */
	private final String _declarationString;

	/**
	 * When inside a parser directive block (p:raw or p:comment), this tracks the directive type.
	 * Null when not inside a directive block.
	 */
	private String _currentDirective;

	/**
	 * Accumulates raw content while inside a parser directive block
	 */
	private StringBuilder _directiveContent;

	/**
	 * Tracks nesting depth of tags with the same name as the current directive, to handle nested occurrences
	 */
	private int _directiveNestingDepth;

	public NGTemplateParser( final String htmlString, final String declarationString ) {
		Objects.requireNonNull( htmlString );
		Objects.requireNonNull( declarationString );

		_htmlString = htmlString;
		_declarationString = declarationString;
	}

	public PNode parse() throws NGDeclarationFormatException, NGHTMLFormatException {

		_declarations = NGDeclarationParser.declarationsWithString( _declarationString );

		return parseHTML();
	}

	private PNode parseHTML() throws NGHTMLFormatException, NGDeclarationFormatException {

		new NGHTMLParser( this, _htmlString ).parseHTML();

		if( _currentDirective != null ) {
			throw new NGHTMLFormatException( "Unclosed parser directive <p:%s>".formatted( _currentDirective ) );
		}

		if( !_currentDynamicTag.isRoot() ) {
			throw new NGHTMLFormatException( "There is an unbalanced dynamic tag named '%s'.".formatted( _currentDynamicTag.declaration().name() ) );
		}

		return new PRootNode( _currentDynamicTag.childrenWithStringsProcessedAndCombined(), SourceRange.EMPTY );
	}

	public void didParseOpeningWebObjectTag( String parsedString ) throws NGHTMLFormatException, NGDeclarationFormatException {

		// If we're inside a directive block, accumulate everything as raw text
		if( _currentDirective != null ) {
			// Check for nested <p:directive> tags to track depth
			if( isParserDirectiveTag( parsedString, _currentDirective ) ) {
				_directiveNestingDepth++;
			}
			_directiveContent.append( parsedString );
			_directiveContent.append( '>' );
			return;
		}

		final int spaceIndex = parsedString.indexOf( ' ' );
		int colonIndex;

		if( spaceIndex != -1 ) {
			colonIndex = parsedString.substring( 0, spaceIndex ).indexOf( ':' );
		}
		else {
			colonIndex = parsedString.indexOf( ':' );
		}

		final boolean isInlineTag = colonIndex != -1;

		// Check if this is a parser directive
		if( isInlineTag ) {
			final String namespace = parsedString.substring( parsedString.indexOf( '<' ) + 1, colonIndex );
			final String tagName = extractTagName( parsedString, colonIndex );

			if( PARSER_NAMESPACE.equals( namespace ) && (DIRECTIVE_RAW.equals( tagName ) || DIRECTIVE_COMMENT.equals( tagName )) ) {
				_currentDirective = tagName;
				_directiveContent = new StringBuilder();
				_directiveNestingDepth = 0;
				return;
			}
		}

		final NGDeclaration declaration;

		if( isInlineTag ) {
			declaration = parseDeclarationFromInlineTag( parsedString, colonIndex, _inlineTagCount++ );
		}
		else {
			final String declarationName = extractDeclarationName( parsedString );
			declaration = _declarations.get( declarationName );

			if( declaration == null ) {
				throw new NGDeclarationFormatException( "No declaration for dynamic element (or component) named '%s'".formatted( declarationName ) );
			}
		}
		_currentDynamicTag = new NGDynamicHTMLTag( declaration, _currentDynamicTag );
	}

	public void didParseClosingWebObjectTag( final String parsedString ) throws NGHTMLFormatException {

		// If we're inside a directive block, check if this is the closing tag for it
		if( _currentDirective != null ) {
			if( isParserDirectiveClosingTag( parsedString, _currentDirective ) ) {
				if( _directiveNestingDepth > 0 ) {
					_directiveNestingDepth--;
					_directiveContent.append( parsedString );
					_directiveContent.append( '>' );
				}
				else {
					// End of directive block
					final String content = _directiveContent.toString();
					final PNode node;

					if( DIRECTIVE_RAW.equals( _currentDirective ) ) {
						node = new PRawNode( content, SourceRange.EMPTY );
					}
					else {
						node = new PCommentNode( content, SourceRange.EMPTY );
					}

					_currentDirective = null;
					_directiveContent = null;
					_currentDynamicTag.addChild( node );
				}
			}
			else {
				_directiveContent.append( parsedString );
				_directiveContent.append( '>' );
			}
			return;
		}

		final NGDynamicHTMLTag parentDynamicTag = _currentDynamicTag.parent();

		if( parentDynamicTag == null ) {
			final String message = "<%s> Unbalanced WebObject tags. Either there is an extra closing </WEBOBJECT> tag in the html template, or one of the opening <WEBOBJECT ...> tag has a typo (extra spaces between a < sign and a WEBOBJECT tag ?).".formatted( getClass().getName() );
			throw new NGHTMLFormatException( message );
		}

		final PNode node = new PBasicNode(
				_currentDynamicTag.declaration().namespace(),
				_currentDynamicTag.declaration().type(),
				_currentDynamicTag.declaration().bindings(),
				_currentDynamicTag.childrenWithStringsProcessedAndCombined(),
				_currentDynamicTag.declaration().isInline(),
				_currentDynamicTag.declaration().name(),
				SourceRange.EMPTY );

		_currentDynamicTag = parentDynamicTag;
		_currentDynamicTag.addChild( node );
	}

	public void didParseText( final String parsedString ) {

		// If we're inside a directive block, accumulate as raw content
		if( _currentDirective != null ) {
			_directiveContent.append( parsedString );
			return;
		}

		_currentDynamicTag.addChild( parsedString );
	}

	/**
	 * Extracts just the tag name from an inline tag string, i.e. the part after the colon up to the first whitespace
	 */
	private static String extractTagName( final String parsedString, final int colonIndex ) {
		final int end = parsedString.indexOf( ' ', colonIndex );

		if( end == -1 ) {
			return parsedString.substring( colonIndex + 1 );
		}

		return parsedString.substring( colonIndex + 1, end );
	}

	/**
	 * @return true if the parsed opening tag string is a parser directive with the given name
	 */
	private static boolean isParserDirectiveTag( final String parsedString, final String directiveName ) {
		final String lowerCase = parsedString.toLowerCase();
		return lowerCase.startsWith( "<p:" + directiveName );
	}

	/**
	 * @return true if the parsed closing tag string closes a parser directive with the given name
	 */
	private static boolean isParserDirectiveClosingTag( final String parsedString, final String directiveName ) {
		final String lowerCase = parsedString.toLowerCase();
		return lowerCase.startsWith( "</p:" + directiveName );
	}

	/**
	 * @return The declaration name (name attribute) from the given dynamic tag
	 */
	static String extractDeclarationName( final String tagPart ) throws NGHTMLFormatException {
		Objects.requireNonNull( tagPart );

		final StringTokenizer st1 = new StringTokenizer( tagPart, "=" );

		if( st1.countTokens() != 2 ) {
			throw new NGHTMLFormatException( "Can't initialize dynamic tag '%s', name=... attribute not found".formatted( tagPart ) );
		}

		st1.nextToken();
		String s1 = st1.nextToken();

		int i = s1.indexOf( '"' );

		// Go here if the attribute name is unquoted
		if( i != -1 ) {
			// this is where we go if the name attribute is quoted
			int j = s1.lastIndexOf( '"' );

			if( j > i ) {
				return s1.substring( i + 1, j );
			}
		}
		else {
			// Assume an unquoted name attributes
			final StringTokenizer st2 = new StringTokenizer( s1 );
			return st2.nextToken();
		}

		throw new NGHTMLFormatException( "Can't initialize dynamic tag '%s', no 'name' attribute found".formatted( tagPart ) );
	}

	private static NGDeclaration parseDeclarationFromInlineTag( final String tag, final int colonIndex, final int nextInlineBindingNumber ) throws NGHTMLFormatException {

		// Extract the namespace (everything between the leading '<' and the colon)
		final String namespace = tag.substring( tag.indexOf( '<' ) + 1, colonIndex );

		final StringBuilder keyBuffer = new StringBuilder();
		final StringBuilder valueBuffer = new StringBuilder();
		final StringBuilder elementTypeBuffer = new StringBuilder();
		final Map<String, NGBindingValue> bindings = new HashMap<>();

		StringBuilder currentBuffer = elementTypeBuffer;
		boolean changeBuffers = false;
		boolean inQuote = false;
		int length = tag.length();

		for( int index = colonIndex + 1; index < length; index++ ) {
			char ch = tag.charAt( index );
			if( !inQuote && (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') ) {
				changeBuffers = true;
			}
			else if( !inQuote && ch == '=' ) {
				changeBuffers = true;
			}
			else if( inQuote && ch == '\\' ) {
				index++;
				if( index == length ) {
					throw new NGHTMLFormatException( "'%s' has a '\\' as the last character.".formatted( tag ) );
				}
				if( tag.charAt( index ) == '\"' ) {
					currentBuffer.append( "\"" );
				}
				else if( tag.charAt( index ) == 'n' ) {
					currentBuffer.append( '\n' );
				}
				else if( tag.charAt( index ) == 'r' ) {
					currentBuffer.append( '\r' );
				}
				else if( tag.charAt( index ) == 't' ) {
					currentBuffer.append( '\t' );
				}
				else {
					currentBuffer.append( '\\' );
					currentBuffer.append( tag.charAt( index ) );
				}
			}
			else {
				if( changeBuffers ) {
					if( currentBuffer == elementTypeBuffer ) {
						currentBuffer = keyBuffer;
					}
					else if( currentBuffer == keyBuffer ) {
						currentBuffer = valueBuffer;
					}
					else if( currentBuffer == valueBuffer ) {
						bindings.put( keyBuffer.toString().trim(), new NGBindingValue.Value( false, valueBuffer.toString().trim() ) );
						currentBuffer = keyBuffer;
					}
					currentBuffer.setLength( 0 );
					changeBuffers = false;
				}
				if( ch == '"' ) {
					inQuote = !inQuote;
				}
				currentBuffer.append( ch );
			}
		}

		if( inQuote ) {
			throw new NGHTMLFormatException( "'%s' has a quote left open.".formatted( tag ) );
		}

		if( keyBuffer.length() > 0 ) {
			if( valueBuffer.length() > 0 ) {
				bindings.put( keyBuffer.toString().trim(), new NGBindingValue.Value( false, valueBuffer.toString().trim() ) );
			}
			else {
				throw new NGHTMLFormatException( "'%s' defines a key but no value.".formatted( tag ) );
			}
		}

		String elementType = elementTypeBuffer.toString();

		// CHECKME: We're probably not going to support this feature. Old parser code kept around for reference // Hugi 2025-03-19
		// if( elementType.startsWith( NGHTMLParser.WO_REPLACEMENT_MARKER ) ) {
		// Acts only on tags, where we have "dynamified" inside the tag parser
		// this takes the value found after the "wo:" part in the element and generates a WOGenericContainer with that value
		// as the elementName binding

		// elementType = elementType.replaceAll( NGHTMLParser.WO_REPLACEMENT_MARKER, "" );
		// bindings.put( "elementName", new NGBindingValue( true, elementType ) );
		// elementType = NGGenericContainer.class.getSimpleName();
		// }

		final String declarationName = "%s_%s".formatted( elementType, nextInlineBindingNumber );
		return new NGDeclaration( true, declarationName, namespace, elementType, bindings );
	}
}
