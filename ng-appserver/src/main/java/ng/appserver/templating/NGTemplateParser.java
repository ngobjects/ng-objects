package ng.appserver.templating;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGAssociation;
import ng.appserver.NGAssociationFactory;
import ng.appserver.NGElement;
import ng.appserver.elements.NGGenericContainer;
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

		// Somewhat ugly hack to prevent the template parser from returning a null template for an empty HTML String (which is not what we want)
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
			final int spaceIndex = parsedString.indexOf( ' ' );
			int colonIndex;

			if( spaceIndex != -1 ) {
				colonIndex = parsedString.substring( 0, spaceIndex ).indexOf( ':' );
			}
			else {
				colonIndex = parsedString.indexOf( ':' );
			}

			if( colonIndex != -1 ) {
				final NGDeclaration declaration = parseInlineBindings( parsedString, colonIndex, _inlineBindingCount++ );
				_declarations.put( declaration.name(), declaration );
				parsedString = "<wo name = \"" + declaration.name() + "\"";
			}
		}

		_currentDynamicTag = new NGDynamicHTMLTag( parsedString, _currentDynamicTag );
	}

	public void didParseClosingWebObjectTag( final String parsedString ) throws NGDeclarationFormatException, NGHTMLFormatException, ClassNotFoundException {
		final NGDynamicHTMLTag parentDynamicTag = _currentDynamicTag.parentTag();

		if( parentDynamicTag == null ) {
			final String message = "<%s> Unbalanced WebObject tags. Either there is an extra closing </WEBOBJECT> tag in the html template, or one of the opening <WEBOBJECT ...> tag has a typo (extra spaces between a < sign and a WEBOBJECT tag ?).".formatted( getClass().getName() );
			throw new NGHTMLFormatException( message );
		}

		// FIXME:
		// That old try/catch gets to stay for a bit, since we still haven't designed a way to catch and report no found dynamic element
		// The methods componentName() and prettyPrintDeclaration() are getting deleted on 2024-10-13, in case you want to have a look at them for reference when encountering this in the future
		// Hugi 2024-10-13
		//		try {
		final NGElement element = _currentDynamicTag.dynamicElement( _declarations, _languages );
		_currentDynamicTag = parentDynamicTag;
		_currentDynamicTag.addChildElement( element );
		//		}
		//		catch( RuntimeException e ) {
		//			final String templ = "Unable to load the component named '%s' with the declaration %s. Make sure the .wo folder is where it's supposed to be and the name is spelled correctly.";
		//			throw new RuntimeException( templ.formatted( componentName( _currentDynamicTag ), prettyPrintDeclaration( _declarations.get( _currentDynamicTag.name() ) ) ), e );
		//		}
	}

	public void didParseComment( final String parsedString ) {
		NGHTMLCommentString commentString = new NGHTMLCommentString( parsedString );
		_currentDynamicTag.addChildElement( commentString );
	}

	public void didParseText( final String parsedString ) {
		_currentDynamicTag.addChildElement( parsedString );
	}

	private static NGDeclaration parseInlineBindings( final String tag, final int colonIndex, final int nextInlineBindingNumber ) throws NGHTMLFormatException {

		final StringBuilder keyBuffer = new StringBuilder();
		final StringBuilder valueBuffer = new StringBuilder();
		final StringBuilder elementTypeBuffer = new StringBuilder();
		final Map<String, NGAssociation> associations = new HashMap<>();

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
						associations.put( keyBuffer.toString().trim(), associationForInlineBindingValue( valueBuffer ) );
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
				associations.put( keyBuffer.toString().trim(), associationForInlineBindingValue( valueBuffer ) );
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
			elementType = elementType.replaceAll( NGHTMLParser.WO_REPLACEMENT_MARKER, "" );
			associations.put( "elementName", NGAssociationFactory.constantValueAssociationWithValue( elementType ) );
			elementType = NGGenericContainer.class.getSimpleName();
		}

		final String declarationName = "_%s_%s".formatted( elementType,nextInlineBindingNumber );
		return new NGDeclaration( declarationName, elementType, associations );
	}

	private static NGAssociation associationForInlineBindingValue( final StringBuilder valueBuffer ) throws NGHTMLFormatException {
		Objects.requireNonNull( valueBuffer );

		String value = valueBuffer.toString().trim();

		final Map<String, String> quotedStrings;

		if( value.startsWith( "\"" ) ) {
			value = value.substring( 1 );

			if( value.endsWith( "\"" ) ) {
				value = value.substring( 0, value.length() - 1 );
			}
			else {
				throw new NGHTMLFormatException( value + " starts with quote but does not end with one." );
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

		return NGAssociationFactory.associationWithValue( value, quotedStrings );
	}

	/**
	 * Indicates if inline bindings (<wo: ...> tags in the HTML) are allowed. Obviously, this defaults to true.
	 */
	private static boolean allowInlineBindings() {
		return true;
	}
}