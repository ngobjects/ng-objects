package ng.appserver.templating;

import java.util.Map;

import ng.appserver.NGAssociation;

public abstract class NGTagProcessor {

	public NGDeclaration createDeclaration( String elementName, String elementType, Map<String, NGAssociation> associations ) {
		return NGTemplateParser.createDeclaration( elementName, elementType, associations );
	}
}