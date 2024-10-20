package ng.appserver.templating;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
	private List<Object> _children;

	public NGDynamicHTMLTag() {
		_parent = null;
		_declaration = null;
	}

	public NGDynamicHTMLTag( final NGDeclaration declaration, final NGDynamicHTMLTag parentTag ) throws NGHTMLFormatException {
		Objects.requireNonNull( declaration );

		_parent = parentTag;
		_declaration = declaration;
	}

	@Deprecated
	public String declarationName() {
		return _declaration.name();
	}

	public NGDynamicHTMLTag parent() {
		return _parent;
	}

	public List<Object> children() {
		return _children;
	}

	public void addChild( final Object stringOrElement ) {
		Objects.requireNonNull( stringOrElement );

		if( _children == null ) {
			_children = new ArrayList<>();
		}

		_children.add( stringOrElement );
	}

	/**
	 * @return true if the given tag is the root of the element tree.
	 *
	 * FIXME:
	 * The implementation of this kind of sucks. The only tag in the tree without a declaration is currently the root tag
	 * We should probably introduce another specific marker for the root tage, or give it a different type. Just anything else than a null check, that's just a consequence of an implementation detail.
	 * // Hugi 2024-10-20
	 */
	public boolean isRoot() {
		return _declaration == null;
	}

	@Override
	public String toString() {
		return "NGDynamicHTMLTag [_declaration=" + _declaration + ",  _children=" + _children + "]";
	}
}