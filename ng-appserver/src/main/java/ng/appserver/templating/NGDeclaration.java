package ng.appserver.templating;

import ng.appserver.NGAssociation;

public class NGDeclaration {

	final String _name;
	final String _type;
	final _NSDictionary<String, NGAssociation> _associations;

	public NGDeclaration( String aName, String aType, _NSDictionary<String, NGAssociation> theAssocations ) {
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

	public _NSDictionary<String, NGAssociation> associations() {
		return _associations;
	}
}