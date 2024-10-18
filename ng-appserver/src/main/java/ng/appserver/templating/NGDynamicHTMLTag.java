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
	private final NGDynamicHTMLTag _parentTag;

	/**
	 * Children of this tag. This list contains a mix of (java) strings and NGElements.
	 */
	private List<Object> _children;

	public NGDynamicHTMLTag() {
		_parentTag = null;
		_declarationName = null;
	}

	public NGDynamicHTMLTag( final String declarationName, final NGDynamicHTMLTag parentTag ) throws NGHTMLFormatException {
		Objects.requireNonNull( declarationName );

		_parentTag = parentTag;
		_declarationName = declarationName;
	}

	public String declarationName() {
		return _declarationName;
	}

	public NGDynamicHTMLTag parentTag() {
		return _parentTag;
	}

	public List<Object> children() {
		return _children;
	}

	public void addChildElement( final Object stringOrElement ) {
		Objects.requireNonNull( stringOrElement );

		if( _children == null ) {
			_children = new ArrayList<>();
		}

		_children.add( stringOrElement );
	}
}