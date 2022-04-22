package ng.appserver.templating;

import ng.appserver.NGAssociation;

public abstract class NGTagProcessor {

	public NGDeclaration createDeclaration( String elementName, String elementType, _NSDictionary<String, NGAssociation> associations ) {
		return NGHelperFunctionParser.createDeclaration( elementName, elementType, associations );
	}
}