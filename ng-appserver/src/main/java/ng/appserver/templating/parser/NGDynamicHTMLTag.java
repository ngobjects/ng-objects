package ng.appserver.templating.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ng.appserver.templating.parser.model.PHTMLNode;
import ng.appserver.templating.parser.model.PNode;

/**
 * Represents a dynamic tag in the HTML part of a template
 */

public class NGDynamicHTMLTag {

	/**
	 * Name of the tag (i.e. the value of the 'name' attribute, that links to the declaration
	 */
	private final NGDeclaration _declaration;

	/**
	 * Parent tag
	 */
	private final NGDynamicHTMLTag _parent;

	/**
	 * Children of this tag. This list contains a mix of (java) strings and PNodes
	 */
	private List<Object> _children = new ArrayList<>();

	public NGDynamicHTMLTag() {
		_parent = null;
		_declaration = null;
	}

	public NGDynamicHTMLTag( final NGDeclaration declaration, final NGDynamicHTMLTag parentTag ) throws NGHTMLFormatException {
		Objects.requireNonNull( declaration );

		_parent = parentTag;
		_declaration = declaration;
	}

	public NGDeclaration declaration() {
		return _declaration;
	}

	public NGDynamicHTMLTag parent() {
		return _parent;
	}

	/**
	 * FIXME: Yeah, we need to fix this in the parser itself // Hugi 2024-11-17
	 */
	@Deprecated
	public List<PNode> childrenWithStringsProcessedAndCombined() {
		return combineAndWrapBareStrings( _children );
	}

	public void addChild( final Object stringOrElement ) {
		Objects.requireNonNull( stringOrElement );
		_children.add( stringOrElement );
	}

	/**
	 * @return true if the given tag is the root of the element tree.
	 *
	 * FIXME:
	 * The only tag in the tree without a declaration is currently assumed to be the root tag, which kind of sucks.
	 * We should probably introduce a specific marker for the root tag or give it a different type.
	 * Just anything else than checking for a null that's only the consequence of an implementation detail.
	 * // Hugi 2024-10-20
	 */
	public boolean isRoot() {
		return _declaration == null;
	}

	/**
	 * Iterates through the list, combining adjacent strings before wrapping them in a PHTMLNode
	 * Other nodes just get added to the list.
	 */
	private static List<PNode> combineAndWrapBareStrings( final List<Object> children ) {
		final List<PNode> childElements = new ArrayList<>( children.size() );

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
					final PHTMLNode bareString = new PHTMLNode( sb.toString() );
					childElements.add( bareString );
					sb.setLength( 0 );
				}

				// ... and then add the element itself
				childElements.add( (PNode)currentChild );
			}
		}

		// If the last element happened to be a string, the StringBuilder will still have data so we wrap it here
		if( sb.length() > 0 ) {
			final PHTMLNode bareString = new PHTMLNode( sb.toString() );
			childElements.add( bareString );
			sb.setLength( 0 );
		}

		return childElements;
	}

	@Override
	public String toString() {
		return "NGDynamicHTMLTag [_declaration=" + _declaration + ",  _children=" + _children + "]";
	}
}