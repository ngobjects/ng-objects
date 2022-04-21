package er.extensions.bettertemplates;

/**
 * WOTagProcessor allows you to munge the associations for a tag declaration. For instance, you could map elementType
 * "not" to a tag processor that returns a WOConditional with the "negate = 'true'" association added to its
 * associations dictionary.
 *
 * @author mschrag
 */
public abstract class NGTagProcessor {
	public NGTagProcessor() {}

	public NGDeclaration createDeclaration( String elementName, String elementType, NSMutableDictionary associations ) {
		return NGHelperFunctionParser.createDeclaration( elementName, elementType, associations );
	}
}
