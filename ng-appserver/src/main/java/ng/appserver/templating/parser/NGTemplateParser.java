package ng.appserver.templating.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import ng.appserver.templating.parser.NGDeclaration.NGBindingValue;
import ng.appserver.templating.parser.NGHTMLParser.CommentType;
import ng.appserver.templating.parser.model.PBasicNode;
import ng.appserver.templating.parser.model.PHTMLComment;
import ng.appserver.templating.parser.model.PLiteralComment;
import ng.appserver.templating.parser.model.PNode;
import ng.appserver.templating.parser.model.PRootNode;
import ng.appserver.templating.parser.model.PTemplateComment;

/**
 * The primary entry point for component parsing
 */

public class NGTemplateParser {

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

		if( !_currentDynamicTag.isRoot() ) {
			throw new NGHTMLFormatException( "There is an unbalanced dynamic tag named '%s'.".formatted( _currentDynamicTag.declaration().name() ) );
		}

		return new PRootNode( _currentDynamicTag.childrenWithStringsProcessedAndCombined() );
	}

	public void didParseOpeningWebObjectTag( String parsedString ) throws NGHTMLFormatException, NGDeclarationFormatException {

		final int spaceIndex = parsedString.indexOf( ' ' );
		int colonIndex;

		if( spaceIndex != -1 ) {
			colonIndex = parsedString.substring( 0, spaceIndex ).indexOf( ':' );
		}
		else {
			colonIndex = parsedString.indexOf( ':' );
		}

		final boolean isInlineTag = colonIndex != -1;

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
				_currentDynamicTag.declaration().name() );

		_currentDynamicTag = parentDynamicTag;
		_currentDynamicTag.addChild( node );
	}

	public void didParseComment( final String parsedString, final CommentType commentType ) {

		final String commentContent = extractCommentContent( parsedString, commentType );

		final PNode commentNode = switch( commentType ) {
			case HTML -> {
				// For HTML comments, parse the inner content as template so dynamic tags inside are processed
				final List<PNode> children = parseCommentContentAsTemplate( commentContent );
				yield new PHTMLComment( children );
			}
			case LITERAL -> new PLiteralComment( commentContent );
			case TEMPLATE -> new PTemplateComment( commentContent );
		};

		_currentDynamicTag.addChild( commentNode );
	}

	public void didParseText( final String parsedString ) {
		_currentDynamicTag.addChild( parsedString );
	}

	/**
	 * Extracts the content between the comment delimiters, stripping the <!-- / <!--! / <!--# prefix and the --> suffix
	 */
	private static String extractCommentContent( final String rawComment, final CommentType commentType ) {
		// Determine prefix length based on comment type
		final int prefixLength = switch( commentType ) {
			case HTML -> 4;     // "<!--"
			case LITERAL -> 5;  // "<!--!"
			case TEMPLATE -> 5; // "<!--#"
		};

		// Strip prefix and the trailing "-->", return content
		final int suffixLength = 3; // "-->"
		return rawComment.substring( prefixLength, rawComment.length() - suffixLength );
	}

	/**
	 * Parses the content of an HTML comment as a template, allowing dynamic tags inside comments to be processed.
	 * Uses a fresh NGTemplateParser instance with the same declarations.
	 */
	private List<PNode> parseCommentContentAsTemplate( final String commentContent ) {
		try {
			final NGTemplateParser subParser = new NGTemplateParser( commentContent, "" );
			subParser._declarations = this._declarations;
			final PNode result = subParser.parseHTML();

			if( result instanceof PRootNode rootNode ) {
				return rootNode.children();
			}

			return List.of( result );
		}
		catch( NGHTMLFormatException | NGDeclarationFormatException e ) {
			// If parsing fails, treat the comment content as a raw text node within the comment
			return List.of( new ng.appserver.templating.parser.model.PHTMLNode( commentContent ) );
		}
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
						bindings.put( keyBuffer.toString().trim(), new NGBindingValue( false, valueBuffer.toString().trim() ) );
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
				bindings.put( keyBuffer.toString().trim(), new NGBindingValue( false, valueBuffer.toString().trim() ) );
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
