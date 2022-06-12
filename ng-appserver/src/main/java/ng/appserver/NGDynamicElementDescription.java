package ng.appserver;

import java.util.List;

import ng.appserver.NGDynamicElementDescription.NGBindingDescription;

/**
 * Provides a description of a dynamic element, particularly what it's bindings are and how they work.
 */

public record NGDynamicElementDescription( Class<? extends NGDynamicElement> elementClass, List<String> tagNames, List<NGBindingDescription> bindings, String text ) {

	public record NGBindingDescription( String name, String text ) {

	}
}