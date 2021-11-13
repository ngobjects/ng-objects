package ognl.helperfunction;

import java.util.Map;

import ng.appserver.NGDeclaration;

/**
 * WOTagProcessor allows you to munge the associations for a tag declaration. For instance, you could map elementType
 * "not" to a tag processor that returns a WOConditional with the "negate = 'true'" association added to its
 * associations dictionary.
 *
 * @author mschrag
 */
public abstract class WOTagProcessor {
	public WOTagProcessor() {
	}

	public NGDeclaration createDeclaration( String elementName, String elementType, Map associations ) {
		return WOHelperFunctionParser.createDeclaration( elementName, elementType, associations );
	}
}
