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

	public NGDynamicHTMLTag() {}

	public NGDynamicHTMLTag( final String tagPart, final NGDynamicHTMLTag parent ) throws NGHTMLFormatException {
		Objects.requireNonNull( tagPart );

		_parent = parent;
		_name = extractDeclarationName( tagPart );
	}

	public String name() {
		return _name;
	}

	public NGDynamicHTMLTag parentTag() {
		return _parent;
	}

	public NGElement template() {
		List list = null;

		if( _children == null ) {
			return null;
		}

		Enumeration<Object> enumeration = Collections.enumeration( _children );

		if( enumeration != null ) {
			list = new ArrayList<>( _children.size() );
			StringBuilder stringb = new StringBuilder( 128 );
			while( enumeration.hasMoreElements() ) {
				Object obj1 = enumeration.nextElement();
				if( obj1 instanceof String ) {
					stringb.append( (String)obj1 );
				}
				else {
					if( stringb.length() > 0 ) {
						NGHTMLBareString bareString = new NGHTMLBareString( stringb.toString() );
						list.add( bareString );
						stringb.setLength( 0 );
					}
					list.add( obj1 );
				}
			}
			if( stringb.length() > 0 ) {
				NGHTMLBareString bareString = new NGHTMLBareString( stringb.toString() );
				stringb.setLength( 0 );
				list.add( bareString );
			}
		}

		NGElement obj = null;

		if( list != null && list.size() == 1 ) {
			Object obj2 = list.get( 0 );
			if( obj2 instanceof NGComponentReference ) {
				obj = new NGDynamicGroup( _name, null, (NGElement)obj2 );
			}
			else {
				obj = (NGElement)obj2;
			}
		}
		else {
			obj = new NGDynamicGroup( _name, null, list );
		}

		return obj;
	}

	public void addChildElement( Object obj ) {
		if( _children == null ) {
			_children = new ArrayList<>();
		}
		_children.add( obj );
	}

	/**
	 * @return The declaration name (name attribute) from the given WebObject tag
	 */
	private static String extractDeclarationName( final String tagPart ) throws NGHTMLFormatException {

		String result = null;

		final StringTokenizer st1 = new StringTokenizer( tagPart, "=" );

		if( st1.countTokens() != 2 ) {
			throw new NGHTMLFormatException( "Can't initialize dynamic tag '%s', name=... attribute not found".formatted( tagPart ) );
		}

		st1.nextToken();
		String s1 = st1.nextToken();

		int i = s1.indexOf( '"' );

		if( i != -1 ) {
			// this is where we go if the name attribute is quoted
			int j = s1.lastIndexOf( '"' );

			if( j > i ) {
				result = s1.substring( i + 1, j );
			}
		}
		else {
			// Assume an unquoted name attributes
			final StringTokenizer st2 = new StringTokenizer( s1 );
			result = st2.nextToken();
		}

		if( result == null ) {
			throw new NGHTMLFormatException( "Can't initialize dynamic tag '%s', no 'name' attribute found".formatted( tagPart ) );
		}

		return result;
	}

	public NGElement dynamicElement( Map<String, NGDeclaration> declarations, List<String> languages ) throws NGDeclarationFormatException, ClassNotFoundException {
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

	private static NGElement _elementWithDeclaration( final NGDeclaration declaration, final String name, final NGElement template, final List<String> languages ) throws ClassNotFoundException, NGDeclarationFormatException {

		if( declaration == null ) {
			throw new NGDeclarationFormatException( "No declaration for dynamic element (or component) named " + name );
		}

		final String typeName = declaration.type();

		if( typeName == null ) {
			throw new NGDeclarationFormatException( "Declaration object for dynamic element (or component) named " + name + "has no class name." );
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