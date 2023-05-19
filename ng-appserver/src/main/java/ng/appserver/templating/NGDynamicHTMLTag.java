package ng.appserver.templating;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import ng.appserver.NGApplication;
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
	 * Children of this tag. This list contains a mix of (java) strings and NGElements.
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

	/**
	 * @return The tag's template
	 */
	public NGElement template() {

		if( _children == null ) {
			return null;
		}

		final List<NGElement> childElements = combineAndWrapBareStringElements( _children );

		if( childElements.size() == 1 ) {
			final NGElement onlyElement = childElements.get( 0 );

			if( onlyElement instanceof NGComponentReference ) {
				return new NGDynamicGroup( _name, null, onlyElement );
			}

			return onlyElement;
		}

		return new NGDynamicGroup( _name, null, childElements );
	}

	/**
	 * Iterates through the list, combining adjacent strings and wrapping them in NGHTMLBareStrings
	 * Other elements get added directly to the element list.
	 */
	private static List<NGElement> combineAndWrapBareStringElements( List<Object> children ) {
		final List<NGElement> childElements = new ArrayList<>( children.size() );

		final StringBuilder sb = new StringBuilder( 128 );

		for( final Object currentChild : children ) {

			if( currentChild instanceof String ) {
				// If we encounter a string, we append it to the StringBuilder
				sb.append( (String)currentChild );
			}
			else {
				// If we encounter any other element and we still have unwrapped strings in our builder,
				// we take the string data we've collected, wrap it up and add it to the element list.
				if( sb.length() > 0 ) {
					final NGHTMLBareString bareString = new NGHTMLBareString( sb.toString() );
					childElements.add( bareString );
					sb.setLength( 0 );
				}

				// ... and then add the element itself
				childElements.add( (NGElement)currentChild );
			}
		}

		// If the last element happened to be a string, the StringBuilder will still have data so we wrap it here
		if( sb.length() > 0 ) {
			final NGHTMLBareString bareString = new NGHTMLBareString( sb.toString() );
			childElements.add( bareString );
			sb.setLength( 0 );
		}

		return childElements;
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
		final NGDeclaration declaration = declarations.get( name() );

		if( declaration == null ) {
			throw new NGDeclarationFormatException( "No declaration for dynamic element (or component) named '%s'".formatted( name() ) );
		}

		return NGApplication.dynamicElementWithName( declaration.type(), declaration.associations(), template(), languages );
	}
}