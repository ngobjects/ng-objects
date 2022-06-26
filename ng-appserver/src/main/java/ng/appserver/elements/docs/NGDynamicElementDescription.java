package ng.appserver.elements.docs;

import java.util.ArrayList;
import java.util.List;

import ng.appserver.NGDynamicElement;
import ng.appserver.elements.NGConditional;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGString;
import ng.appserver.elements.docs.NGDynamicElementDescription.NGBindingDescription;

/**
 * Provides a description of a dynamic element, particularly what it's bindings are and how they work.
 *
 * FIXME: Specify required bindings/binding combinations
 * FIXME: Specify default values for bindings
 */

public record NGDynamicElementDescription( Class<? extends NGDynamicElement> elementClass, List<String> aliases, List<NGBindingDescription> bindings, String text ) {

	public record NGBindingDescription( String name, String text ) {}

	/**
	 * @return true if this is a container element
	 *
	 * FIXME: We're making the grand assumption that all container elements inherit from NGDynamicGroup. This is not always true, for example for components (which might end up documented with the same API)
	 */
	public boolean isContainerElement() {
		return NGDynamicGroup.class.isAssignableFrom( elementClass );
	}

	public static List<NGDynamicElementDescription> all() {
		final ArrayList<NGDynamicElementDescription> list = new ArrayList<>();

		list.add( new NGDynamicElementDescription(
				NGConditional.class,
				List.of( "if" ),
				List.of(
						new NGBindingDescription( "condition", "The condition to evaluate" ),
						new NGBindingDescription( "negate", "Can be set to $true to 'flip' the condition" ) ),
				"If the binding [condition] evaluates to $false, the contained content will not be rendered (and vice versa). If the 'negate' binding is set to $true, the condition will be flipped." ) );

		list.add( new NGDynamicElementDescription(
				NGImage.class,
				List.of( "img" ),
				List.of(
						new NGBindingDescription( "filename", "Path to a webserver resource" ),
						new NGBindingDescription( "src", "Same as using an src attribute on a regular img tag" ),
						new NGBindingDescription( "data", "byte array containing image data" ) ),
				"Displays an image. Bindings that are not part of the elements standard associations are passed on as attributes to the generated img tag." ) );

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