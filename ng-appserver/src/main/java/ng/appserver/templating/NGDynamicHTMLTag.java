package ng.appserver.templating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGComponentDefinition;
import ng.appserver.NGComponentReference;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.elements.NGHTMLBareString;

/**
 * Represents a dynamic tag in the HTML part of a template
 */

public class NGDynamicHTMLTag {

	/**
	 * Name of the tag (i.e. the value of the 'name' attribute, that links to the declaration
	 */
	private String _name;

	/**
	 * Parent tag
	 */
	private NGDynamicHTMLTag _parent;

	/**
	 * Children of this tag
	 */
	private List<Object> _children;

	public NGDynamicHTMLTag() {
		_name = null;
	}

	public NGDynamicHTMLTag( final String tagPart, final NGDynamicHTMLTag parent ) throws NGHelperFunctionHTMLFormatException {
		Objects.requireNonNull( tagPart );

		_parent = parent;
		extractName( tagPart );
	}

	public String name() {
		return _name;
	}

	public NGDynamicHTMLTag parentTag() {
		return _parent;
	}

	public NGElement template() {
		List nsmutablearray = null;

		if( _children == null ) {
			return null;
		}

		Enumeration<Object> enumeration = Collections.enumeration( _children );

		if( enumeration != null ) {
			nsmutablearray = new ArrayList<>( _children.size() );
			StringBuilder stringb = new StringBuilder( 128 );
			while( enumeration.hasMoreElements() ) {
				Object obj1 = enumeration.nextElement();
				if( obj1 instanceof String ) {
					stringb.append( (String)obj1 );
				}
				else {
					if( stringb.length() > 0 ) {
						NGHTMLBareString wohtmlbarestring1 = new NGHTMLBareString( stringb.toString() );
						nsmutablearray.add( wohtmlbarestring1 );
						stringb.setLength( 0 );
					}
					nsmutablearray.add( obj1 );
				}
			}
			if( stringb.length() > 0 ) {
				NGHTMLBareString wohtmlbarestring = new NGHTMLBareString( stringb.toString() );
				stringb.setLength( 0 );
				nsmutablearray.add( wohtmlbarestring );
			}
		}

		NGElement obj = null;

		if( nsmutablearray != null && nsmutablearray.size() == 1 ) {
			Object obj2 = nsmutablearray.get( 0 );
			if( obj2 instanceof NGComponentReference ) {
				obj = new NGDynamicGroup( _name, null, (NGElement)obj2 );
			}
			else {
				obj = (NGElement)obj2;
			}
		}
		else {
			obj = new NGDynamicGroup( _name, null, nsmutablearray );
		}

		return obj;
	}

	public void addChildElement( Object obj ) {
		if( _children == null ) {
			_children = new ArrayList<>();
		}
		_children.add( obj );
	}

	private void extractName( String s ) throws NGHelperFunctionHTMLFormatException {

		StringTokenizer stringtokenizer = new StringTokenizer( s, "=" );
		if( stringtokenizer.countTokens() != 2 ) {
			throw new NGHelperFunctionHTMLFormatException( "<WOHTMLWebObjectTag cannot initialize WebObject tag " + s + "> . It has no NAME=... parameter" );
		}

		stringtokenizer.nextToken();
		String s1 = stringtokenizer.nextToken();

		int i = s1.indexOf( '"' );
		if( i != -1 ) {
			int j = s1.lastIndexOf( '"' );
			if( j > i ) {
				_name = s1.substring( i + 1, j );
			}
		}
		else {
			StringTokenizer stringtokenizer1 = new StringTokenizer( s1 );
			_name = stringtokenizer1.nextToken();
		}

		if( _name == null ) {
			throw new NGHelperFunctionHTMLFormatException( "<WOHTMLWebObjectTag cannot initialize WebObject tag " + s + "> . Failed parsing NAME parameter" );
		}
	}

	public NGElement dynamicElement( Map<String, NGDeclaration> declarations, List<String> languages ) throws NGHelperFunctionDeclarationFormatException, ClassNotFoundException {
		final String name = name();
		final NGElement woelement = template();
		final NGDeclaration wodeclaration = declarations.get( name );
		return _elementWithDeclaration( wodeclaration, name, woelement, languages );
	}

	private static NGElement _componentReferenceWithClassNameDeclarationAndTemplate( String s, NGDeclaration wodeclaration, NGElement woelement, List<String> languages ) throws ClassNotFoundException {
		NGComponentReference wocomponentreference = null;
		NGComponentDefinition wocomponentdefinition = NGApplication.application()._componentDefinition( s, languages );

		if( wocomponentdefinition != null ) {
			Map<String, NGAssociation> nsdictionary = wodeclaration.associations();
			wocomponentreference = wocomponentdefinition.componentReferenceWithAssociations( nsdictionary, woelement );
		}
		else {
			throw new ClassNotFoundException( "Cannot find class or component named \'" + s + "\" in runtime or in a loadable bundle" );
		}

		return wocomponentreference;
	}

	private static NGElement _elementWithClass( Class<? extends NGElement> c, NGDeclaration declaration, NGElement element ) {
		return NGApplication.application().dynamicElementWithName( c.getName(), declaration.associations(), element, null );
	}

	private static NGElement _elementWithDeclaration( final NGDeclaration declaration, final String name, final NGElement template, final List<String> languages ) throws ClassNotFoundException, NGHelperFunctionDeclarationFormatException {

		if( declaration == null ) {
			throw new NGHelperFunctionDeclarationFormatException( "<WOHTMLTemplateParser> no declaration for dynamic element (or component) named " + name );
		}

		final String typeName = declaration.type();

		if( typeName == null ) {
			throw new NGHelperFunctionDeclarationFormatException( "<WOHTMLWebObjectTag> declaration object for dynamic element (or component) named " + name + "has no class name." );
		}

		Class<? extends NGElement> classForTypeName = _NGUtilities.classWithName( typeName );

		if( classForTypeName == null ) {
			classForTypeName = _NGUtilities.lookForClassInAllBundles( typeName );

			if( classForTypeName == null ) {
				//						logger.info( "WOBundle.lookForClassInAllBundles(" + s1 + ") failed!" );
			}
			else if( !(NGDynamicElement.class).isAssignableFrom( classForTypeName ) ) {
				classForTypeName = null;
			}
		}

		if( classForTypeName != null ) {
			if( (NGComponent.class).isAssignableFrom( classForTypeName ) ) {
				return _componentReferenceWithClassNameDeclarationAndTemplate( typeName, declaration, template, languages );
			}
			else {
				return _elementWithClass( classForTypeName, declaration, template );
			}
		}

		return _componentReferenceWithClassNameDeclarationAndTemplate( typeName, declaration, template, languages );
	}
}