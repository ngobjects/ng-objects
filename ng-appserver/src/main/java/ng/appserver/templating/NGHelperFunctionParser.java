package ng.appserver.templating;

import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAssociation;
import ng.appserver.NGConstantValueAssociation;
import ng.appserver.NGElement;
import ng.appserver.NGKeyValueAssociation;
import ng.appserver.elements.NGHTMLCommentString;

public class NGHelperFunctionParser {
	private static final Logger log = LoggerFactory.getLogger( NGHelperFunctionParser.class );

	public static final String APP_FRAMEWORK_NAME = "app";

	public static boolean _debugSupport;

	private static String WO_REPLACEMENT_MARKER = "__REPL__";

	private NGHTMLWebObjectTag _currentWebObjectTag;
	private _NSMutableDictionary _declarations;
	private int _inlineBindingCount;

	private String _declarationString;
	private String _HTMLString;
	private List _languages;

	public NGHelperFunctionParser( String htmlString, String declarationString, List languages ) {
		_HTMLString = htmlString;
		_declarationString = declarationString;
		_languages = languages;
		_declarations = null;
		_currentWebObjectTag = new NGHTMLWebObjectTag();
	}

	public NGElement parse() throws NGHelperFunctionDeclarationFormatException, NGHelperFunctionHTMLFormatException, ClassNotFoundException {
		parseDeclarations();
		for( Enumeration e = declarations().objectEnumerator(); e.hasMoreElements(); ) {
			NGDeclaration declaration = (NGDeclaration)e.nextElement();
			processDeclaration( declaration );
		}
		NGElement woelement = parseHTML();
		return woelement;
	}

	public static boolean allowInlineBindings() {
		return true;
	}

	public void didParseOpeningWebObjectTag( String s, NGHelperFunctionHTMLParser htmlParser ) throws NGHelperFunctionHTMLFormatException {
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
		_currentWebObjectTag = new NGHTMLWebObjectTag( s, _currentWebObjectTag );
		log.debug( "Inserted WebObject with Name '{}'.", _currentWebObjectTag.name() );
	}

	public void didParseClosingWebObjectTag( String s, NGHelperFunctionHTMLParser htmlParser ) throws NGHelperFunctionDeclarationFormatException, NGHelperFunctionHTMLFormatException, ClassNotFoundException {
		NGHTMLWebObjectTag webobjectTag = _currentWebObjectTag.parentTag();
		if( webobjectTag == null ) {
			throw new NGHelperFunctionHTMLFormatException( "<" + getClass().getName() + "> Unbalanced WebObject tags. Either there is an extra closing </WEBOBJECT> tag in the html template, or one of the opening <WEBOBJECT ...> tag has a typo (extra spaces between a < sign and a WEBOBJECT tag ?)." );
		}
		try {
			NGElement element = _currentWebObjectTag.dynamicElement( _declarations, _languages );
			_currentWebObjectTag = webobjectTag;
			_currentWebObjectTag.addChildElement( element );
		}
		catch( RuntimeException e ) {
			throw new RuntimeException( "Unable to load the component named '" + componentName( _currentWebObjectTag ) + "' with the declaration " + prettyDeclaration( (NGDeclaration)_declarations.get( _currentWebObjectTag.name() ) ) + ". Make sure the .wo folder is where it's supposed to be and the name is spelled correctly.", e );
		}
	}

	public void didParseComment( String comment, NGHelperFunctionHTMLParser htmlParser ) {
		NGHTMLCommentString wohtmlcommentstring = new NGHTMLCommentString( comment );
		_currentWebObjectTag.addChildElement( wohtmlcommentstring );
	}

	public void didParseText( String text, NGHelperFunctionHTMLParser htmlParser ) {
		_currentWebObjectTag.addChildElement( text );
	}

	protected NGDeclaration parseInlineBindings( String tag, int colonIndex ) throws NGHelperFunctionHTMLFormatException {
		StringBuffer keyBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		StringBuffer elementTypeBuffer = new StringBuffer();
		_NSMutableDictionary associations = new _NSMutableDictionary();
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
					throw new NGHelperFunctionHTMLFormatException( "'" + tag + "' has a '\\' as the last character." );
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
			throw new NGHelperFunctionHTMLFormatException( "'" + tag + "' has a quote left open." );
		}

		if( keyBuffer.length() > 0 ) {
			if( valueBuffer.length() > 0 ) {
				parseInlineAssociation( keyBuffer, valueBuffer, associations );
			}
			else {
				throw new NGHelperFunctionHTMLFormatException( "'" + tag + "' defines a key but no value." );
			}
		}
		String elementType = elementTypeBuffer.toString();
		String shortcutType = NGHelperFunctionTagRegistry.tagShortcutMap().get( elementType );
		if( shortcutType != null ) {
			elementType = shortcutType;
		}
		else if( elementType.startsWith( WO_REPLACEMENT_MARKER ) ) {
			// Acts only on tags, where we have "dynamified" inside the tag parser
			// this takes the value found after the "wo:" part in the element and generates a WOGenericContainer with that value
			// as the elementName binding
			elementType = elementType.replaceAll( WO_REPLACEMENT_MARKER, "" );
			associations.put( "elementName", NGHelperFunctionAssociation.associationWithValue( elementType ) );
			elementType = "WOGenericContainer";
		}
		String elementName;
		synchronized( this ) {
			elementName = "_" + elementType + "_" + _inlineBindingCount;
			_inlineBindingCount++;
		}
		NGTagProcessor tagProcessor = NGHelperFunctionTagRegistry.tagProcessorMap().get( elementType );
		NGDeclaration declaration;
		if( tagProcessor == null ) {
			declaration = NGHelperFunctionParser.createDeclaration( elementName, elementType, associations );
		}
		else {
			declaration = tagProcessor.createDeclaration( elementName, elementType, associations );
		}
		_declarations.put( elementName, declaration );
		processDeclaration( declaration );
		return declaration;
	}

	protected void parseInlineAssociation( StringBuffer keyBuffer, StringBuffer valueBuffer, _NSMutableDictionary bindings ) throws NGHelperFunctionHTMLFormatException {
		String key = keyBuffer.toString().trim();
		String value = valueBuffer.toString().trim();
		_NSDictionary quotedStrings;
		if( value.startsWith( "\"" ) ) {
			value = value.substring( 1 );
			if( value.endsWith( "\"" ) ) {
				value = value.substring( 0, value.length() - 1 );
			}
			else {
				throw new NGHelperFunctionHTMLFormatException( valueBuffer + " starts with quote but does not end with one." );
			}
			if( value.startsWith( "$" ) ) {
				value = value.substring( 1 );
				if( value.endsWith( "VALID" ) ) {
					value = value.replaceFirst( "\\s*//\\s*VALID", "" );
				}
				quotedStrings = new _NSDictionary();
			}
			else {
				value = value.replaceAll( "\\\\\\$", "\\$" );
				value = value.replaceAll( "\\\"", "\"" );
				quotedStrings = new _NSDictionary( value, "_WODP_0" );
				value = "_WODP_0";
			}
		}
		else {
			quotedStrings = new _NSDictionary();
		}
		NGAssociation association = NGHelperFunctionDeclarationParser._associationWithKey( value, quotedStrings );
		bindings.put( key, association );
	}

	protected void processDeclaration( NGDeclaration declaration ) {
		_NSMutableDictionary associations = (_NSMutableDictionary)declaration.associations();
		Enumeration bindingNameEnum = associations.keyEnumerator();
		while( bindingNameEnum.hasMoreElements() ) {
			String bindingName = (String)bindingNameEnum.nextElement();
			NGAssociation association = (NGAssociation)associations.get( bindingName );
			NGAssociation helperAssociation = parserHelperAssociation( association );
			if( helperAssociation != association ) {
				associations.put( bindingName, helperAssociation );
			}
		}
	}

	protected NGAssociation parserHelperAssociation( NGAssociation originalAssociation ) {
		NGAssociation association = originalAssociation;
		String originalKeyPath = null;
		if( association instanceof NGKeyValueAssociation ) {
			NGKeyValueAssociation kvAssociation = (NGKeyValueAssociation)association;
			originalKeyPath = kvAssociation.keyPath();
		}
		// else if (association instanceof WOConstantValueAssociation) {
		// WOConstantValueAssociation constantAssociation =
		// (WOConstantValueAssociation) association;
		// Object constantValue = constantAssociation.valueInComponent(null);
		// if (constantValue instanceof String) {
		// originalKeyPath = (String) constantValue;
		// }
		// }

		if( originalKeyPath != null ) {
			int pipeIndex = originalKeyPath.indexOf( '|' );
			if( pipeIndex != -1 ) {
				String targetKeyPath = originalKeyPath.substring( 0, pipeIndex ).trim();
				String frameworkName = APP_FRAMEWORK_NAME;
				String helperFunctionName = originalKeyPath.substring( pipeIndex + 1 ).trim();
				String otherParams = null;
				int openParenIndex = helperFunctionName.indexOf( '(' );
				if( openParenIndex != -1 ) {
					int closeParenIndex = helperFunctionName.indexOf( ')', openParenIndex + 1 );
					otherParams = helperFunctionName.substring( openParenIndex + 1, closeParenIndex );
					helperFunctionName = helperFunctionName.substring( 0, openParenIndex );
				}
				int helperFunctionDotIndex = helperFunctionName.indexOf( '.' );
				if( helperFunctionDotIndex != -1 ) {
					frameworkName = helperFunctionName.substring( 0, helperFunctionDotIndex );
					helperFunctionName = helperFunctionName.substring( helperFunctionDotIndex + 1 );
				}
				StringBuilder newKeyPath = new StringBuilder();
				newKeyPath.append( '~' );
				//				newKeyPath.append( "@" + NGHelperFunctionRegistry.class.getName() + "@registry()._helperInstanceForFrameworkNamed(#this, \"" ); // WTF?
				newKeyPath.append( helperFunctionName );
				newKeyPath.append( "\", \"" );
				newKeyPath.append( targetKeyPath );
				newKeyPath.append( "\", \"" );
				newKeyPath.append( frameworkName );
				newKeyPath.append( "\")." );
				newKeyPath.append( helperFunctionName );
				newKeyPath.append( '(' );
				newKeyPath.append( targetKeyPath );
				if( otherParams != null ) {
					newKeyPath.append( ',' );
					newKeyPath.append( otherParams );
				}
				newKeyPath.append( ')' );
				log.debug( "Converted {} into {}", originalKeyPath, newKeyPath );
				association = new NGConstantValueAssociation( newKeyPath.toString() );
			}
		}
		return association;
	}

	protected String prettyDeclaration( NGDeclaration declaration ) {
		StringBuilder declarationStr = new StringBuilder();
		if( declaration == null ) {
			declarationStr.append( "[none]" );
		}
		else {
			declarationStr.append( "Component Type = " + declaration.type() );
			declarationStr.append( ", Bindings = { " );
			Enumeration keyEnum = declaration.associations().keyEnumerator();
			while( keyEnum.hasMoreElements() ) {
				String key = (String)keyEnum.nextElement();
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

	private NGElement parseHTML() throws NGHelperFunctionHTMLFormatException, NGHelperFunctionDeclarationFormatException, ClassNotFoundException {
		NGElement currentWebObjectTemplate = null;
		if( _HTMLString != null && _declarations != null ) {
			NGHelperFunctionHTMLParser htmlParser = new NGHelperFunctionHTMLParser( this, _HTMLString );
			htmlParser.parseHTML();
			String webobjectTagName = _currentWebObjectTag.name();
			if( webobjectTagName != null ) {
				throw new NGHelperFunctionHTMLFormatException( "There is an unbalanced WebObjects tag named '" + webobjectTagName + "'." );
			}
			currentWebObjectTemplate = _currentWebObjectTag.template();
		}
		return currentWebObjectTemplate;
	}

	protected boolean isInline( NGHTMLWebObjectTag tag ) {
		String name = tag.name();
		return name != null && name.startsWith( "_" ) && name.length() > 1 && name.indexOf( '_', 1 ) != -1;
	}

	protected String componentName( NGHTMLWebObjectTag tag ) {
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

	public String declarationString() {
		return _declarationString;
	}

	public void setDeclarationString( String value ) {
		_declarationString = value;
	}

	public _NSMutableDictionary declarations() {
		return _declarations;
	}

	public void setDeclarations( _NSMutableDictionary value ) {
		_declarations = value;
	}

	private void parseDeclarations() throws NGHelperFunctionDeclarationFormatException {
		if( _declarations == null && _declarationString != null ) {
			_declarations = NGHelperFunctionDeclarationParser.declarationsWithString( _declarationString );
		}
	}

	public static NGDeclaration createDeclaration( String declarationName, String declarationType, _NSMutableDictionary associations ) {
		NGDeclaration declaration = new NGDeclaration( declarationName, declarationType, associations );

		if( NGHelperFunctionParser._debugSupport && associations != null /*&& associations.objectForKey( NGHTMLAttribute.Debug ) == null */ ) {
			//associations.setObjectForKey(new WOConstantValueAssociation(Boolean.TRUE), WOHTMLAttribute.Debug);
			Enumeration associationsEnum = associations.keyEnumerator();
			while( associationsEnum.hasMoreElements() ) {
				String bindingName = (String)associationsEnum.nextElement();
				NGAssociation association = (NGAssociation)associations.get( bindingName );
				association.setDebugEnabledForBinding( bindingName, declarationName, declarationType );
				association._setDebuggingEnabled( false );
			}
		}

		return declaration;
	}
}
