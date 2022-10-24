package ng.appserver.elements.docs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import ng.appserver.NGDynamicElement;
import ng.appserver.elements.NGComponentContent;
import ng.appserver.elements.NGConditional;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.elements.NGForm;
import ng.appserver.elements.NGGenericContainer;
import ng.appserver.elements.NGGenericElement;
import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGJavaScript;
import ng.appserver.elements.NGRepetition;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;
import ng.appserver.elements.NGSubmitButton;
import ng.appserver.elements.NGTextField;
import ng.appserver.elements.docs.NGDynamicElementDescription.NGBindingDescription;
import ng.appserver.templating.NGElementUtils;

/**
 * Provides a description of a dynamic element, particularly what it's bindings are and how they work.
 *
 * FIXME: Specify required bindings/binding combinations
 * FIXME: Specify default values for bindings
 * FIXME: Specify binding directionality, i.e. if they are pull/push or both.
 * FIXME: Specify allowed binding types
 */

public record NGDynamicElementDescription( Class<? extends NGDynamicElement> elementClass, List<String> aliases, List<NGBindingDescription> bindings, String text ) {

	/**
	 * Represents missing description for a dynamic element.
	 */
	public static NGDynamicElementDescription NoDescription = new NGDynamicElementDescription( null, null, null, null );

	/**
	 * FIXME: We should be using Collections.emptyList() but KVC has a problem with it. Look into later // Hugi 2022-10-13
	 */
	public static NGDynamicElementDescription createEmptyDescription( Class<? extends NGDynamicElement> elementClass ) {
		return new NGDynamicElementDescription( elementClass, new ArrayList<>(), new ArrayList<>(), "Documentation forthcoming" );
	}

	/**
	 * Describes a binding
	 */
	public record NGBindingDescription( String name, Class<?> bindingClass, String text ) {}

	/**
	 * @return true if this is a container element
	 *
	 * FIXME: We're making the grand assumption that all container elements inherit from NGDynamicGroup. This is not always true, for example for components (which might end up documented with the same API)
	 */
	public boolean isContainerElement() {
		return NGDynamicGroup.class.isAssignableFrom( elementClass() );
	}

	/**
	 * @return The list of shortcuts for the tag. FIXME: Butt-ugly
	 */
	public List<String> aliases() {
		final List<String> result = new ArrayList<>();

		for( Entry<String, String> entry : NGElementUtils.tagShortcutMap().entrySet() ) {
			if( entry.getValue().equals( elementClass().getSimpleName() ) ) {
				result.add( entry.getKey() );
			}
		}

		return result;
	}

	public static List<NGDynamicElementDescription> all() {
		final ArrayList<NGDynamicElementDescription> list = new ArrayList<>();

		list.add( createEmptyDescription( NGComponentContent.class ) );

		list.add( new NGDynamicElementDescription(
				NGConditional.class,
				List.of( "if" ),
				List.of(
						new NGBindingDescription( "condition", Object.class, "The condition to evaluate" ),
						new NGBindingDescription( "negate", Boolean.class, "Can be set to $true to 'flip' the condition" ) ),
				"Wraps content in a template and decides to render it based on a condition. If the binding [condition] evaluates to $false, the contained content will not be rendered (and vice versa). If the 'negate' binding is set to $true, the condition will be flipped." ) );

		list.add( createEmptyDescription( NGForm.class ) );
		list.add( createEmptyDescription( NGGenericContainer.class ) );
		list.add( createEmptyDescription( NGGenericElement.class ) );
		list.add( createEmptyDescription( NGHyperlink.class ) );

		list.add( new NGDynamicElementDescription(
				NGImage.class,
				List.of( "img" ),
				List.of(
						new NGBindingDescription( "filename", String.class, "Path to a webserver resource" ),
						new NGBindingDescription( "src", String.class, "Same as using an src attribute on a regular img tag" ),
						new NGBindingDescription( "data", byte[].class, "byte array containing image data" ) ),
				"Displays an image. Bindings that are not part of the elements standard associations are passed on as attributes to the generated img tag." ) );

		list.add( createEmptyDescription( NGJavaScript.class ) );

		list.add( new NGDynamicElementDescription(
				NGRepetition.class,
				List.of( "repetition" ),
				List.of(
						new NGBindingDescription( "list", List.class, "A java.util.List of objects to iterate over" ),
						new NGBindingDescription( "item", Object.class, "Takes the value of the object currently being iterated over" ),
						new NGBindingDescription( "index", Integer.class, "Takes the number of the current iteration. Zero based, i.e. the first iteration is zero." ),
						new NGBindingDescription( "count", Integer.class, "Can be used instead of [list] and [item] to just iterate [count] times" ) ),
				"Iterates over items in [list], with [item] taking on the value of the object for each iteration. Or iterates [count] times. If [index] is bound, that variable will take on the current index." ) );

		list.add( new NGDynamicElementDescription(
				NGString.class,
				List.of( "str" ),
				List.of(
						new NGBindingDescription( "value", Object.class, "The value to display. If not a string, toString() will be invoked on th object to render it" ),
						new NGBindingDescription( "escapeHTML", Object.class, "Indicates if you want to convert reserved HTML characters to entity values for display (currently &lt; and &gt;). Defaults to true" ), // FIXME: Update docs when we've figured out other elements to escape
						new NGBindingDescription( "valueWhenEmpty", Object.class, "The value to display if [value] is null or empty (zero length string)" ) ),
				"Renders to a string in a template. If anything other than a string gets passed to [value] it, toString() will be invoked on it to render it." ) );

		list.add( createEmptyDescription( NGStylesheet.class ) );
		list.add( createEmptyDescription( NGSubmitButton.class ) );
		list.add( createEmptyDescription( NGTextField.class ) );

		return list;
	}
}