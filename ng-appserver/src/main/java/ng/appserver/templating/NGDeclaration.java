package ng.appserver.templating;

import java.util.Map;

import ng.appserver.NGAssociation;

public class NGDeclaration {

	final String _name;
	final String _type;
	final Map<String, NGAssociation> _associations;

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