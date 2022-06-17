package ng.appserver.elements.docs;

import java.util.ArrayList;
import java.util.List;

import ng.appserver.NGDynamicElement;
import ng.appserver.elements.NGString;
import ng.appserver.elements.docs.NGDynamicElementDescription.NGBindingDescription;

/**
 * Provides a description of a dynamic element, particularly what it's bindings are and how they work.
 */

public record NGDynamicElementDescription( Class<? extends NGDynamicElement> elementClass, List<String> aliases, List<NGBindingDescription> bindings, String text ) {

	public record NGBindingDescription( String name, String text ) {}

	public static List<NGDynamicElementDescription> all() {
		final ArrayList<NGDynamicElementDescription> list = new ArrayList<>();

		list.add( new NGDynamicElementDescription(
				NGString.class,
				List.of( "str" ),
				List.of(
						new NGBindingDescription( "value", "The string's value" ),
						new NGBindingDescription( "valueWhenEmpty", "A string to show if the string is blank or empty" ) ),
				"For rendering strings " ) );

		return list;
	}

}