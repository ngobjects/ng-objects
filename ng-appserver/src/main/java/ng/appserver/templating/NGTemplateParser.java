package ng.appserver.templating;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import ng.appserver.templating.NGDeclaration.NGBindingValue;

/**
 * The primary entry point for component parsing
 */

public class NGTemplateParser {

	/**
	 * Our context, i.e. the dynamic tag currently being parsed
	 */
	private NGDynamicHTMLTag _currentDynamicTag = new NGDynamicHTMLTag();

	/**
	 * Keeps track of declarations. Will initially contain the parsed declarationString (if present) and any inline bindings will get added here as well.
	 */
	private Map<String, NGDeclaration> _declarations;

	/**
	 * Keeps track of how many inline tags have been parsed. Used only to generate the tag declaration's name.
	 */
	private int _inlineBindingCount;

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

		// Somewhat ugly hack to prevent the template parser from returning a null template for an empty HTML String (which is not what we want)
		if( _htmlString.isEmpty() ) {
			return new PHTMLNode( "" );
		}

		_declarations = NGDeclarationParser.declarationsWithString( _declarationString );

		return parseHTML();
	}

	private PNode parseHTML() throws NGHTMLFormatException, NGDeclarationFormatException {

		new NGHTMLParser( this, _htmlString ).parseHTML();

		final String currentDynamicTagName = _currentDynamicTag.declarationName();

		if( currentDynamicTagName != null ) {
			throw new NGHTMLFormatException( "There is an unbalanced dynamic tag named '%s'.".formatted( currentDynamicTagName ) );
		}

		return new PGroupNode( _currentDynamicTag );
	}

	public void didParseOpeningWebObjectTag( String parsedString ) throws NGHTMLFormatException {

		final int spaceIndex = parsedString.indexOf( ' ' );
		int colonIndex;

		if( spaceIndex != -1 ) {
			colonIndex = parsedString.substring( 0, spaceIndex ).indexOf( ':' );
		}
		else {
			colonIndex = parsedString.indexOf( ':' );
		}

		final boolean isInlineTag = colonIndex != -1;

		if( isInlineTag ) {
			final NGDeclaration declaration = parseInlineTag( parsedString, colonIndex, _inlineBindingCount++ );
			_declarations.put( declaration.name(), declaration );
			_currentDynamicTag = new NGDynamicHTMLTag( declaration.name(), _currentDynamicTag );
		}
		else {
			_currentDynamicTag = new NGDynamicHTMLTag( extractDeclarationName( parsedString ), _currentDynamicTag );
		}
	}

	public void didParseClosingWebObjectTag( final String parsedString ) throws NGDeclarationFormatException, NGHTMLFormatException {
		final NGDynamicHTMLTag parentDynamicTag = _currentDynamicTag.parentTag();

		if( parentDynamicTag == null ) {
			final String message = "<%s> Unbalanced WebObject tags. Either there is an extra closing </WEBOBJECT> tag in the html template, or one of the opening <WEBOBJECT ...> tag has a typo (extra spaces between a < sign and a WEBOBJECT tag ?).".formatted( getClass().getName() );
			throw new NGHTMLFormatException( message );
		}

		final PNode node = node( _currentDynamicTag, _declarations );
		_currentDynamicTag = parentDynamicTag;
		_currentDynamicTag.addChild( node );
	}

	public void didParseComment( final String parsedString ) {
		PCommentNode commentString = new PCommentNode( parsedString );
		_currentDynamicTag.addChild( commentString );
	}

	public void didParseText( final String parsedString ) {
		_currentDynamicTag.addChild( parsedString );
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

	public static PNode node( NGDynamicHTMLTag tag, final Map<String, NGDeclaration> declarations ) throws NGDeclarationFormatException {
		final NGDeclaration declaration = declarations.get( tag.declarationName() );

		if( declaration == null ) {
			throw new NGDeclarationFormatException( "No declaration for dynamic element (or component) named '%s'".formatted( tag.declarationName() ) );
		}

		return new PBasicNode( tag, declaration );
	}

	private static NGDeclaration parseInlineTag( final String tag, final int colonIndex, final int nextInlineBindingNumber ) throws NGHTMLFormatException {

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
					throw new NGHTMLFormatException( "'" + tag + "' has a '\\' as the last character." );
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
			throw new NGHTMLFormatException( "'" + tag + "' has a quote left open." );
		}

		if( keyBuffer.length() > 0 ) {
			if( valueBuffer.length() > 0 ) {
				bindings.put( keyBuffer.toString().trim(), new NGBindingValue( false, valueBuffer.toString().trim() ) );
			}
			else {
				throw new NGHTMLFormatException( "'" + tag + "' defines a key but no value." );
			}
		}

		String elementType = elementTypeBuffer.toString();

		if( elementType.startsWith( NGHTMLParser.WO_REPLACEMENT_MARKER ) ) {
			// Acts only on tags, where we have "dynamified" inside the tag parser
			// this takes the value found after the "wo:" part in the element and generates a WOGenericContainer with that value
			// as the elementName binding

			// FIXME: Fix up in "New parser world"
			//			elementType = elementType.replaceAll( NGHTMLParser.WO_REPLACEMENT_MARKER, "" );
			//			bindings.put( "elementName", new NGBindingValue( true, elementType ) );
			//			elementType = NGGenericContainer.class.getSimpleName();
		}

		final String declarationName = "_%s_%s".formatted( elementType, nextInlineBindingNumber );
		return new NGDeclaration( true, declarationName, elementType, bindings );
	}
}