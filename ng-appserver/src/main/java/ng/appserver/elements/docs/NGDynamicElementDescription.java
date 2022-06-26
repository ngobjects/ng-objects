package ng.appserver.elements.docs;

import java.util.ArrayList;
import java.util.List;

import ng.appserver.NGDynamicElement;
import ng.appserver.elements.NGImage;
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
				NGImage.class,
				List.of( "img" ),
				List.of(
						new NGBindingDescription( "filename", "Path to a webserver resource" ),
						new NGBindingDescription( "src", "Same as using an src attribute on a regular img tag" ),
						new NGBindingDescription( "data", "byte array containing image data" ) ),
				"Displays an image. Bindings that are not part of the elements standard associations are passed on as attributes to the img tag generated." ) );

		list.add( new NGDynamicElementDescription(
				NGString.class,
				List.of( "str" ),
				List.of(
						new NGBindingDescription( "value", "The string's value" ),
						new NGBindingDescription( "valueWhenEmpty", "A string to show if the string is blank or empty" ) ),
				"Renders to a string in a template. If anything other than a string gets passed to [value] it, toString() will be invoked on it to render it." ) );

		return list;
	}
}