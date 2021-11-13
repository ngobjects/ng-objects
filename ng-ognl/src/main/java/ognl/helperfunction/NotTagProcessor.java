package ognl.helperfunction;

import java.util.Map;

import ng.appserver.NGConstantValueAssociation;
import ng.appserver.NGDeclaration;

/**
 * "not" tag processor. This is a shortcut for a WOConditional with negate = 'true'. All you set is "condition".
 *
 * @author mschrag
 */
public class NotTagProcessor extends WOTagProcessor {

	@Override
	public NGDeclaration createDeclaration( String elementName, String elementType, Map associations ) {
		String newElementType = "ERXWOConditional";

		if( associations.get( "negate" ) != null ) {
			throw new IllegalArgumentException( "You already specified a binding for 'negate' of " + associations.get( "negate" ) + " on a wo:not." );
		}

		associations.put( "negate", new NGConstantValueAssociation( Boolean.TRUE ) );
		return super.createDeclaration( elementName, newElementType, associations );
	}
}
