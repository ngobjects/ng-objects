package ng.appserver.templating;

public abstract class NGTagProcessor {

	public NGDeclaration createDeclaration( String elementName, String elementType, _NSDictionary associations ) {
		return NGHelperFunctionParser.createDeclaration( elementName, elementType, associations );
	}
}