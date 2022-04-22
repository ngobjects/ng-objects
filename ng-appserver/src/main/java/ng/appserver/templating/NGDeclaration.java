package ng.appserver.templating;

import java.util.Map;

import ng.appserver.NGAssociation;

/**
 * Represents a declaration in a .wod file
 */

public class NGDeclaration {

	private final String _name;
	private final String _type;
	private final Map<String, NGAssociation> _associations;

	public NGDeclaration( String aName, String aType, Map<String, NGAssociation> theAssocations ) {
		_name = aName;
		_type = aType;
		_associations = theAssocations;
	}

	public String name() {
		return _name;
	}

	public String type() {
		return _type;
	}

	public Map<String, NGAssociation> associations() {
		return _associations;
	}
}