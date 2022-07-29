package ng.appserver.templating;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGComponentDefinition;
import ng.appserver.NGComponentReference;
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
	private final String _name;

	/**
	 * Parent tag
	 */
	private final NGDynamicHTMLTag _parentTag;

	/**
	 * Children of this tag
	 */
	private List<Object> _children;

	public NGDynamicHTMLTag() {
		_parentTag = null;
		_name = null;
	}

	public NGDynamicHTMLTag( final String tagPart, final NGDynamicHTMLTag parentTag ) throws NGHTMLFormatException {
		Objects.requireNonNull( tagPart );

		_parentTag = parentTag;
		_name = extractDeclarationName( tagPart );
	}

	public String name() {
		return _name;
	}

	public NGDynamicHTMLTag parentTag() {
		return _parentTag;
	}

	public NGElement template() {

		if( _children == null ) {
			return null;
		}

		final List list = new ArrayList<>( _children.size() );
		final StringBuilder sb = new StringBuilder( 128 );

		for( final Object currentChild : _children ) {

			if( currentChild instanceof String ) {
				sb.append( (String)currentChild );
			}
			else {
				if( sb.length() > 0 ) {
					NGHTMLBareString bareString = new NGHTMLBareString( sb.toString() );
					list.add( bareString );
					sb.setLength( 0 );
				}

				list.add( currentChild );
			}
		}

		if( sb.length() > 0 ) {
			NGHTMLBareString bareString = new NGHTMLBareString( sb.toString() );
			sb.setLength( 0 );
			list.add( bareString );
		}

		NGElement template;

		if( list.size() == 1 ) {
			final NGElement onlyElement = (NGElement)list.get( 0 );

			if( onlyElement instanceof NGComponentReference ) {
				template = new NGDynamicGroup( _name, null, onlyElement );
			}
			else {
				template = onlyElement;
			}
		}
		else {
			template = new NGDynamicGroup( _name, null, list );
		}

		return template;
	}

	public void addChildElement( final Object stringOrElement ) {
		Objects.requireNonNull( stringOrElement );

		if( _children == null ) {
			_children = new ArrayList<>();
		}

		_children.add( stringOrElement );
	}

	/**
	 * @return The declaration name (name attribute) from the given dynamic tag
	 */
	private static String extractDeclarationName( final String tagPart ) throws NGHTMLFormatException {
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

	public NGElement dynamicElement( final Map<String, NGDeclaration> declarations, final List<String> languages ) throws NGDeclarationFormatException, ClassNotFoundException {
		final String name = name();
		final NGElement element = template();
		final NGDeclaration declaration = declarations.get( name );
		return _elementWithDeclaration( declaration, name, element, languages );
	}

	/**
	 * FIXME: We're kind of duplicating some functionality here from NGApplication. We're going to want to consolidate this with the functionality already found there // Hugi 2022-06-06
	 */
	private static NGElement _elementWithDeclaration( final NGDeclaration declaration, final String name, final NGElement contentTemplate, final List<String> languages ) throws ClassNotFoundException, NGDeclarationFormatException {

		if( declaration == null ) {
			throw new NGDeclarationFormatException( "No declaration for dynamic element (or component) named " + name );
		}

		final String typeName = declaration.type();

		if( typeName == null ) {
			throw new NGDeclarationFormatException( "Declaration object for dynamic element (or component) named " + name + "has no class name." );
		}

		Class<? extends NGElement> classForTypeName = _NGUtilities.classWithName( typeName );

		if( classForTypeName != null ) {
			if( NGComponent.class.isAssignableFrom( classForTypeName ) ) {
				return _componentReferenceWithName( typeName, declaration, contentTemplate, languages );
			}
			else {
				return _dynamicElementWithName( classForTypeName, declaration, contentTemplate );
			}
		}

		return _componentReferenceWithName( typeName, declaration, contentTemplate, languages );
	}

	private static NGElement _componentReferenceWithName( final String componentName, final NGDeclaration declaration, final NGElement contentTemplate, final List<String> languages ) throws ClassNotFoundException {
		final NGComponentDefinition componentDefinition = NGApplication.application()._componentDefinition( componentName, languages );

		if( componentDefinition == null ) {
			throw new ClassNotFoundException( "Cannot find class or component named \'" + componentName + "\" in runtime or in a loadable bundle" );
		}

		final Map<String, NGAssociation> associations = declaration.associations();
		return componentDefinition.componentReferenceWithAssociations( associations, contentTemplate );
	}

	private static NGElement _dynamicElementWithName( final Class<? extends NGElement> elementClass, final NGDeclaration declaration, final NGElement contentTemplate ) {
		return NGApplication.application().dynamicElementWithName( elementClass.getName(), declaration.associations(), contentTemplate, null );
	}
}