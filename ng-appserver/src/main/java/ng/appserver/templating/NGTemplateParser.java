package ng.appserver.templating;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGAssociation;
import ng.appserver.NGConstantValueAssociation;
import ng.appserver.NGElement;
import ng.appserver.NGKeyValueAssociation;
import ng.appserver.elements.NGHTMLCommentString;

/**
 * The primary entry point for component parsing
 */

public class NGTemplateParser {

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

	public void didParseOpeningWebObjectTag( String tagString, NGHTMLParser htmlParser ) throws NGHTMLFormatException {

		if( allowInlineBindings() ) {
			int spaceIndex = tagString.indexOf( ' ' );
			int colonIndex;

			if( spaceIndex != -1 ) {
				colonIndex = tagString.substring( 0, spaceIndex ).indexOf( ':' );
			}
			else {
				colonIndex = tagString.indexOf( ':' );
			}

			if( colonIndex != -1 ) {
				NGDeclaration declaration = parseInlineBindings( tagString, colonIndex );
				tagString = "<wo name = \"" + declaration.name() + "\"";
			}
		}

		_currentDynamicTag = new NGDynamicHTMLTag( tagString, _currentDynamicTag );
	}

	public void didParseClosingWebObjectTag( String s, NGHTMLParser htmlParser ) throws NGDeclarationFormatException, NGHTMLFormatException, ClassNotFoundException {
		final NGDynamicHTMLTag dynamicTag = _currentDynamicTag.parentTag();

		if( dynamicTag == null ) {
			throw new NGHTMLFormatException( "<" + getClass().getName() + "> Unbalanced WebObject tags. Either there is an extra closing </WEBOBJECT> tag in the html template, or one of the opening <WEBOBJECT ...> tag has a typo (extra spaces between a < sign and a WEBOBJECT tag ?)." );
		}

		try {
			final NGElement element = _currentDynamicTag.dynamicElement( _declarations, _languages );
			_currentDynamicTag = dynamicTag;
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
		final StringBuffer keyBuffer = new StringBuffer();
		final StringBuffer valueBuffer = new StringBuffer();
		final StringBuffer elementTypeBuffer = new StringBuffer();
		final Map<String, NGAssociation> associations = new HashMap<>();

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

		final NGDeclaration declaration = NGTemplateParser.createDeclaration( elementName, elementType, associations );

		_declarations.put( elementName, declaration );

		return declaration;
	}

	private static void parseInlineAssociation( final StringBuffer keyBuffer, final StringBuffer valueBuffer, final Map<String, NGAssociation> bindings ) throws NGHTMLFormatException {
		Objects.requireNonNull( keyBuffer );
		Objects.requireNonNull( valueBuffer );
		Objects.requireNonNull( bindings );

		final String key = keyBuffer.toString().trim();
		String value = valueBuffer.toString().trim();

		final Map<String, String> quotedStrings;

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

		final NGAssociation association = NGDeclarationParser._associationWithKey( value, quotedStrings );
		bindings.put( key, association );
	}

	/**
	 * Only used to create a pretty string for debug logging
	 */
	private static String prettyDeclaration( final NGDeclaration declaration ) {

		if( declaration == null ) {
			return "[none]";
		}

		final StringBuilder sb = new StringBuilder();
		sb.append( "Component Type = " + declaration.type() );
		sb.append( ", Bindings = { " );
		Enumeration<String> keyEnum = Collections.enumeration( declaration.associations().keySet() );

		while( keyEnum.hasMoreElements() ) {
			String key = keyEnum.nextElement();
			Object assoc = declaration.associations().get( key );
			if( assoc instanceof NGKeyValueAssociation ) {
				sb.append( key + "=" + ((NGKeyValueAssociation)assoc).keyPath() );
			}
			else if( assoc instanceof NGConstantValueAssociation ) {
				sb.append( key + "='" + ((NGConstantValueAssociation)assoc).valueInComponent( null ) + "'" );
			}
			else {
				sb.append( key + "=" + assoc );
			}
			if( keyEnum.hasMoreElements() ) {
				sb.append( ", " );
			}
		}

		sb.append( " }" );

		return sb.toString();
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

	private static boolean isInline( final NGDynamicHTMLTag tag ) {
		Objects.requireNonNull( tag );

		String name = tag.name();
		return name != null && name.startsWith( "_" ) && name.length() > 1 && name.indexOf( '_', 1 ) != -1;
	}

	private static String componentName( final NGDynamicHTMLTag tag ) {
		Objects.requireNonNull( tag );

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

	public static NGDeclaration createDeclaration( final String declarationName, final String declarationType, final Map<String, NGAssociation> associations ) {
		Objects.requireNonNull( declarationName );
		Objects.requireNonNull( declarationType );
		Objects.requireNonNull( associations );

		return new NGDeclaration( declarationName, declarationType, associations );
	}

	/**
	 * Indicates if inline bindings (<wo: ...> tags in the HTML) are allowed. Obviously, this defaults to true.
	 */
	private static boolean allowInlineBindings() {
		return true;
	}
}