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
import ng.appserver.elements.NGHTMLBareString;
import ng.appserver.elements.NGHTMLCommentString;

/**
 * The primary entry point for component parsing
 *
 * FIXME: Identify bug that caused me to have to add a space (or any other character) before the starting <span> generic container in NGExceptionPage
 */

public class NGTemplateParser {

	/**
	 * Our context, i.e. the dynamic tag currently being parsed
	 */
	private NGDynamicHTMLTag _currentDynamicTag = new NGDynamicHTMLTag();

	/**
	 * The parsed declarationString
	 */
	private Map<String, NGDeclaration> _declarations;

	/**
	 * Keeps track of how many inline bindings have been parsed, used in the generated declaration's name
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

	/**
	 * List of languages. Used when constructing element instances for the template.
	 */
	private final List<String> _languages;

	private NGTemplateParser( final String htmlString, final String declarationString, final List<String> languages ) {
		_htmlString = htmlString;
		_declarationString = declarationString;
		_languages = languages;
	}

	public static NGElement parse( final String htmlString, final String declarationString, final List<String> languages ) throws NGDeclarationFormatException, NGHTMLFormatException, ClassNotFoundException {
		return new NGTemplateParser( htmlString, declarationString, languages ).parse();
	}

	private NGElement parse() throws NGDeclarationFormatException, NGHTMLFormatException, ClassNotFoundException {

		// FIXME: This is a somewhat ugly hack for the template parser returning a null template for an empty HTML String (which is not what we want) // Hugi 2022-06-09
		if( _htmlString.isEmpty() ) {
			return new NGHTMLBareString( "" );
		}

		parseDeclarations();
		return parseHTML();
	}

	private void parseDeclarations() throws NGDeclarationFormatException {
		if( _declarationString != null ) {
			_declarations = NGDeclarationParser.declarationsWithString( _declarationString );
		}
	}

	private NGElement parseHTML() throws NGHTMLFormatException, NGDeclarationFormatException, ClassNotFoundException {

		new NGHTMLParser( this, _htmlString ).parseHTML();

		final String currentDynamicTagName = _currentDynamicTag.name();

		if( currentDynamicTagName != null ) {
			throw new NGHTMLFormatException( "There is an unbalanced dynamic tag named '%s'.".formatted( currentDynamicTagName ) );
		}

		return _currentDynamicTag.template();
	}

	public void didParseOpeningWebObjectTag( String parsedString ) throws NGHTMLFormatException {

		if( allowInlineBindings() ) {
			int spaceIndex = parsedString.indexOf( ' ' );
			int colonIndex;

			if( spaceIndex != -1 ) {
				colonIndex = parsedString.substring( 0, spaceIndex ).indexOf( ':' );
			}
			else {
				colonIndex = parsedString.indexOf( ':' );
			}

			if( colonIndex != -1 ) {
				NGDeclaration declaration = parseInlineBindings( parsedString, colonIndex );
				parsedString = "<wo name = \"" + declaration.name() + "\"";
			}
		}

		_currentDynamicTag = new NGDynamicHTMLTag( parsedString, _currentDynamicTag );
	}

	public void didParseClosingWebObjectTag( final String parsedString ) throws NGDeclarationFormatException, NGHTMLFormatException, ClassNotFoundException {
		final NGDynamicHTMLTag dynamicTag = _currentDynamicTag.parentTag();

		if( dynamicTag == null ) {
			final String message = "<%s> Unbalanced WebObject tags. Either there is an extra closing </WEBOBJECT> tag in the html template, or one of the opening <WEBOBJECT ...> tag has a typo (extra spaces between a < sign and a WEBOBJECT tag ?).".formatted( getClass().getName() );
			throw new NGHTMLFormatException( message );
		}

		try {
			final NGElement element = _currentDynamicTag.dynamicElement( _declarations, _languages );
			_currentDynamicTag = dynamicTag;
			_currentDynamicTag.addChildElement( element );
		}
		catch( RuntimeException e ) { // FIXME: Catching RuntimeException feels super weird // Hugi 2022-10-07
			final String templ = "Unable to load the component named '%s' with the declaration %s. Make sure the .wo folder is where it's supposed to be and the name is spelled correctly.";
			throw new RuntimeException( templ.formatted( componentName( _currentDynamicTag ), prettyPrintDeclaration( _declarations.get( _currentDynamicTag.name() ) ) ), e );
		}
	}

	public void didParseComment( final String parsedString ) {
		NGHTMLCommentString commentString = new NGHTMLCommentString( parsedString );
		_currentDynamicTag.addChildElement( commentString );
	}

	public void didParseText( final String parsedString ) {
		_currentDynamicTag.addChildElement( parsedString );
	}

	private NGDeclaration parseInlineBindings( final String tag, final int colonIndex ) throws NGHTMLFormatException {

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
		final String shortcutType = _NGUtilities.tagShortcutMap().get( elementType );

		if( shortcutType != null ) {
			elementType = shortcutType;
		}
		else if( elementType.startsWith( NGHTMLParser.WO_REPLACEMENT_MARKER ) ) {
			// Acts only on tags, where we have "dynamified" inside the tag parser
			// this takes the value found after the "wo:" part in the element and generates a WOGenericContainer with that value
			// as the elementName binding
			elementType = elementType.replaceAll( NGHTMLParser.WO_REPLACEMENT_MARKER, "" );
			associations.put( "elementName", NGAssociationFactory.associationWithValue( elementType ) );
			elementType = "NGGenericContainer"; // FIXME: This element name might change // Hugi 2022-04-27
		}

		String elementName;

		synchronized( this ) {
			elementName = "_" + elementType + "_" + _inlineBindingCount;
			_inlineBindingCount++;
		}

		final NGDeclaration declaration = NGDeclaration.create( elementName, elementType, associations );

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

	/**
	 * @return A pretty string representation of an NGDeclaration for debug logging
	 */
	private static String prettyPrintDeclaration( final NGDeclaration declaration ) {

		if( declaration == null ) {
			return "[none]";
		}

		final StringBuilder sb = new StringBuilder();
		sb.append( "Component Type = " + declaration.type() );
		sb.append( ", Bindings = { " );

		final Enumeration<String> keyEnum = Collections.enumeration( declaration.associations().keySet() );

		while( keyEnum.hasMoreElements() ) {
			final String key = keyEnum.nextElement();
			final Object association = declaration.associations().get( key );

			if( association instanceof NGKeyValueAssociation ) {
				sb.append( key + "=" + ((NGKeyValueAssociation)association).keyPath() );
			}
			else if( association instanceof NGConstantValueAssociation ) {
				sb.append( key + "='" + ((NGConstantValueAssociation)association).valueInComponent( null ) + "'" );
			}
			else {
				sb.append( key + "=" + association );
			}

			if( keyEnum.hasMoreElements() ) {
				sb.append( ", " );
			}
		}

		sb.append( " }" );

		return sb.toString();
	}

	/**
	 * Indicates if inline bindings (<wo: ...> tags in the HTML) are allowed. Obviously, this defaults to true.
	 */
	private static boolean allowInlineBindings() {
		return true;
	}
}