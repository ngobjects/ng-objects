package ng.appserver.templating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.elements.NGComponentContent;
import ng.appserver.elements.NGConditional;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.elements.NGForm;
import ng.appserver.elements.NGGenericContainer;
import ng.appserver.elements.NGGenericElement;
import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGJavaScript;
import ng.appserver.elements.NGPopUpButton;
import ng.appserver.elements.NGRepetition;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;
import ng.appserver.elements.NGSubmitButton;
import ng.appserver.elements.NGSwitchComponent;
import ng.appserver.elements.NGTextField;

/**
 * A description of a dynamic element and it's bindings. The eventual goal of this class is to combine:
 *
 * 1) The documentation of a dynamic element and it's bindings, usable for both publication and for display inline (in an IDE)
 * 2) To provide runtime validation of binding configuration
 *
 * FIXME: Add support for binding validation
 * FIXME: Required bindings/valid binding combinations
 * FIXME: Allow marking a binding as deprecated (including an explanation/docs, e.g, what bindings to use instead)
 * FIXME: Default values for bindings (i.e. what it defaults to if the binding is not bound)
 * FIXME: Specify binding directionality, i.e. if they pull/push or both.
 * FIXME: Specify types allowed to be passed to a binding
 * FIXME: And if an element pushes values, _what_ do they push. An element might accept a variety of types for a binding but always push back a single type (e.g. a textfield will take whatever and use whatever.toString() - but always push back a String (well, unless you use a formatter, which is an entire different story)
 * FIXME: Support "sets" of valid values that can be passed to a binding, i.e, a list of values, the legal values of a certain enum etc.
 */

public record NGDynamicElementDescription( Class<? extends NGDynamicElement> elementClass, List<String> aliases, List<NGBindingDescription> bindings, String text ) {

	/**
	 * Represents missing description for a dynamic element.
	 */
	public static NGDynamicElementDescription NoDescription = new NGDynamicElementDescription( null, null, null, null );

	/**
	 * Represents missing documentation for an element. "Documentation forthcoming" added to poke the PTSD of veteran WO developers.
	 */
	public static NGDynamicElementDescription createEmptyDescription( Class<? extends NGDynamicElement> elementClass ) {
		return new NGDynamicElementDescription( elementClass, Collections.emptyList(), Collections.emptyList(), "Documentation forthcoming" );
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
	 * @return The list of shortcuts for the tag, as defined by the framework core.
	 */
	public List<String> aliases() {
		final List<String> result = new ArrayList<>();

		final Map<String, String> tagNameMap = NGApplication.application().elementManager().elementTagNames();

		for( Entry<String, String> entry : tagNameMap.entrySet() ) {
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

		list.add( new NGDynamicElementDescription(
				NGForm.class,
				List.of( "form" ),
				List.of(
						new NGBindingDescription( "action", NGActionResults.class, "The action to invoke when the form is submitted" ),
						new NGBindingDescription( "method", String.class, "The form's method (POST or GET). Defaults to 'POST'" ) ),
				"An HTML form, specifically for use with component actions" ) );
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
		list.add( createEmptyDescription( NGPopUpButton.class ) );

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
						new NGBindingDescription( "escapeHTML", Object.class, "Indicates if you want to escape reserved HTML characters to entity values (&lt;, &gt;, single quote and double quote). Defaults to true" ),
						new NGBindingDescription( "valueWhenEmpty", Object.class, "The value to display if [value] is null or empty (zero length string)" ) ),
				"Renders to a string in a template. If anything other than a string gets passed to [value] it, toString() will be invoked on it to render it." ) );

		list.add( createEmptyDescription( NGStylesheet.class ) );
		list.add( createEmptyDescription( NGSubmitButton.class ) );
		list.add( createEmptyDescription( NGSwitchComponent.class ) );
		list.add( createEmptyDescription( NGTextField.class ) );

		return list;
	}
}