package ng.appserver.templating;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAssociation;
import ng.appserver.NGConstantValueAssociation;
import ng.appserver.NGElement;
import ng.appserver.NGKeyValueAssociation;
import ng.appserver.elements.NGHTMLCommentString;

/**
 * The primary entry point for component parsing
 */

public class NGTemplateParser {

	private static final Logger logger = LoggerFactory.getLogger( NGTemplateParser.class );

	private static String WO_REPLACEMENT_MARKER = "__REPL__";

	private NGDynamicHTMLTag _currentDynamicTag = new NGDynamicHTMLTag(); // FIXME: Do we need to set this on initialization?
	private Map<String, NGDeclaration> _declarations;
	private int _inlineBindingCount;

	private final String _HTMLString;
	private final String _declarationString;
	private final List<String> _languages;

	public NGTemplateParser( final String htmlString, final String declarationString, final List<String> languages ) {
		_HTMLString = htmlString;
		_declarationString = declarationString;
		_languages = languages;
	}

	public NGElement parse() throws NGDeclarationFormatException, NGHTMLFormatException, ClassNotFoundException {
		parseDeclarations();
		return parseHTML();
	}

	public void didParseOpeningWebObjectTag( String s, NGHTMLParser htmlParser ) throws NGHTMLFormatException {
		if( allowInlineBindings() ) {
			int spaceIndex = s.indexOf( ' ' );
			int colonIndex;
			if( spaceIndex != -1 ) {
				colonIndex = s.substring( 0, spaceIndex ).indexOf( ':' );
			}
			else {
				colonIndex = s.indexOf( ':' );
			}
			if( colonIndex != -1 ) {
				NGDeclaration declaration = parseInlineBindings( s, colonIndex );
				s = "<wo name = \"" + declaration.name() + "\"";
			}
		}
		_currentDynamicTag = new NGDynamicHTMLTag( s, _currentDynamicTag );
		logger.debug( "Inserted dynamic tag with Name '{}'.", _currentDynamicTag.name() );
	}

	public void didParseClosingWebObjectTag( String s, NGHTMLParser htmlParser ) throws NGDeclarationFormatException, NGHTMLFormatException, ClassNotFoundException {
		NGDynamicHTMLTag webobjectTag = _currentDynamicTag.parentTag();
		if( webobjectTag == null ) {
			throw new NGHTMLFormatException( "<" + getClass().getName() + "> Unbalanced WebObject tags. Either there is an extra closing </WEBOBJECT> tag in the html template, or one of the opening <WEBOBJECT ...> tag has a typo (extra spaces between a < sign and a WEBOBJECT tag ?)." );
		}
		try {
			NGElement element = _currentDynamicTag.dynamicElement( _declarations, _languages );
			_currentDynamicTag = webobjectTag;
			_currentDynamicTag.addChildElement( element );
		}
		catch( RuntimeException e ) {
			throw new RuntimeException( "Unable to load the component named '" + componentName( _currentDynamicTag ) + "' with the declaration " + prettyDeclaration( _declarations.get( _currentDynamicTag.name() ) ) + ". Make sure the .wo folder is where it's supposed to be and the name is spelled correctly.", e );
		}
	}

	public void didParseComment( String comment, NGHTMLParser htmlParser ) {
		NGHTMLCommentString commentString = new NGHTMLCommentString( comment );
		_currentDynamicTag.addChildElement( commentString );
	}

	public void didParseText( String text, NGHTMLParser htmlParser ) {
		_currentDynamicTag.addChildElement( text );
	}

	private NGDeclaration parseInlineBindings( String tag, int colonIndex ) throws NGHTMLFormatException {
		StringBuffer keyBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		StringBuffer elementTypeBuffer = new StringBuffer();
		Map<String, NGAssociation> associations = new HashMap<>();
		StringBuffer currentBuffer = elementTypeBuffer;
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
						parseInlineAssociation( keyBuffer, valueBuffer, associations );
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
				parseInlineAssociation( keyBuffer, valueBuffer, associations );
			}
			else {
				throw new NGHTMLFormatException( "'" + tag + "' defines a key but no value." );
			}
		}
		String elementType = elementTypeBuffer.toString();
		String shortcutType = _NGUtilities.tagShortcutMap().get( elementType );
		if( shortcutType != null ) {
			elementType = shortcutType;
		}
		else if( elementType.startsWith( WO_REPLACEMENT_MARKER ) ) {
			// Acts only on tags, where we have "dynamified" inside the tag parser
			// this takes the value found after the "wo:" part in the element and generates a WOGenericContainer with that value
			// as the elementName binding
			elementType = elementType.replaceAll( WO_REPLACEMENT_MARKER, "" );
			associations.put( "elementName", NGAssociationFactory.associationWithValue( elementType ) );
			elementType = "WOGenericContainer";
		}

		String elementName;

		synchronized( this ) {
			elementName = "_" + elementType + "_" + _inlineBindingCount;
			_inlineBindingCount++;
		}

		final NGTagProcessor tagProcessor = tagProcessorMap().get( elementType );
		NGDeclaration declaration;

		if( tagProcessor == null ) {
			declaration = NGTemplateParser.createDeclaration( elementName, elementType, associations );
		}
		else {
			declaration = tagProcessor.createDeclaration( elementName, elementType, associations );
		}

		_declarations.put( elementName, declaration );

		return declaration;
	}

	private static void parseInlineAssociation( StringBuffer keyBuffer, StringBuffer valueBuffer, Map<String, NGAssociation> bindings ) throws NGHTMLFormatException {
		String key = keyBuffer.toString().trim();
		String value = valueBuffer.toString().trim();
		Map<String, String> quotedStrings;
		if( value.startsWith( "\"" ) ) {
			value = value.substring( 1 );
			if( value.endsWith( "\"" ) ) {
				value = value.substring( 0, value.length() - 1 );
			}
			else {
				throw new NGHTMLFormatException( valueBuffer + " starts with quote but does not end with one." );
			}
			if( value.startsWith( "$" ) ) {
				value = value.substring( 1 );
				if( value.endsWith( "VALID" ) ) {
					value = value.replaceFirst( "\\s*//\\s*VALID", "" );
				}
				quotedStrings = new HashMap<>();
			}
			else {
				value = value.replaceAll( "\\\\\\$", "\\$" );
				value = value.replaceAll( "\\\"", "\"" );
				quotedStrings = new HashMap<>();
				quotedStrings.put( "_WODP_0", value );
				value = "_WODP_0";
			}
		}
		else {
			quotedStrings = new HashMap<>();
		}
		NGAssociation association = NGDeclarationParser._associationWithKey( value, quotedStrings );
		bindings.put( key, association );
	}

	/**
	 * Only used for debug logging
	 */
	private static String prettyDeclaration( NGDeclaration declaration ) {
		StringBuilder declarationStr = new StringBuilder();

		if( declaration == null ) {
			declarationStr.append( "[none]" );
		}
		else {
			declarationStr.append( "Component Type = " + declaration.type() );
			declarationStr.append( ", Bindings = { " );
			Enumeration<String> keyEnum = Collections.enumeration( declaration.associations().keySet() );
			while( keyEnum.hasMoreElements() ) {
				String key = keyEnum.nextElement();
				Object assoc = declaration.associations().get( key );
				if( assoc instanceof NGKeyValueAssociation ) {
					declarationStr.append( key + "=" + ((NGKeyValueAssociation)assoc).keyPath() );
				}
				else if( assoc instanceof NGConstantValueAssociation ) {
					declarationStr.append( key + "='" + ((NGConstantValueAssociation)assoc).valueInComponent( null ) + "'" );
				}
				else {
					declarationStr.append( key + "=" + assoc );
				}
				if( keyEnum.hasMoreElements() ) {
					declarationStr.append( ", " );
				}
			}
			declarationStr.append( " }" );
		}

		return declarationStr.toString();
	}

	private NGElement parseHTML() throws NGHTMLFormatException, NGDeclarationFormatException, ClassNotFoundException {
		NGElement currentWebObjectTemplate = null;
		if( _HTMLString != null && _declarations != null ) {
			NGHTMLParser htmlParser = new NGHTMLParser( this, _HTMLString );
			htmlParser.parseHTML();
			String webobjectTagName = _currentDynamicTag.name();
			if( webobjectTagName != null ) {
				throw new NGHTMLFormatException( "There is an unbalanced WebObjects tag named '" + webobjectTagName + "'." );
			}
			currentWebObjectTemplate = _currentDynamicTag.template();
		}
		return currentWebObjectTemplate;
	}

	private static boolean isInline( NGDynamicHTMLTag tag ) {
		String name = tag.name();
		return name != null && name.startsWith( "_" ) && name.length() > 1 && name.indexOf( '_', 1 ) != -1;
	}

	private static String componentName( final NGDynamicHTMLTag tag ) {
		String name = tag.name();

		// This goofiness reparses back out inline binding names
		if( name == null ) {
			name = "[none]";
		}
		else if( isInline( tag ) ) {
			int secondUnderscoreIndex = name.indexOf( '_', 1 );

			if( secondUnderscoreIndex != -1 ) {
				name = name.substring( 1, secondUnderscoreIndex );
			}
		}

		return name;
	}

	private void parseDeclarations() throws NGDeclarationFormatException {
		if( _declarations == null && _declarationString != null ) {
			_declarations = NGDeclarationParser.declarationsWithString( _declarationString );
		}
	}

	public static NGDeclaration createDeclaration( String declarationName, String declarationType, Map<String, NGAssociation> associations ) {
		return new NGDeclaration( declarationName, declarationType, associations );
	}

	/**
	 * Indicates if inline bindings (<wo: ...> tags in the HTML) are allowed. Obviously, this defaults to true.
	 */
	private static boolean allowInlineBindings() {
		return true;
	}

	/**
	 * A map of tag processors. Not sure if we should keep this around, but it's here for a while at least // Hugi 2022-04-23
	 */
	private static Map<String, NGTagProcessor> tagProcessorMap() {
		return Collections.emptyMap();
	}
}