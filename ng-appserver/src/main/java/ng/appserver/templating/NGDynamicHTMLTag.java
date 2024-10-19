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
	private final String _declarationName;

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
		_declarationName = null;
	}

	public NGDynamicHTMLTag( final String declarationName, final NGDynamicHTMLTag parentTag ) throws NGHTMLFormatException {
		Objects.requireNonNull( declarationName );

		_parent = parentTag;
		_declarationName = declarationName;
	}

	public String declarationName() {
		return _declarationName;
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

	@Override
	public String toString() {
		return "NGDynamicHTMLTag [_declarationName=" + _declarationName + ",  _children=" + _children + "]";
	}
}