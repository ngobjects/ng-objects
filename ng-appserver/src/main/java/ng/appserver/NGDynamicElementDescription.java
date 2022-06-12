package ng.appserver;

import java.util.List;

import ng.appserver.NGDynamicElementDescription.NGBindingDescription;

public record NGDynamicElementDescription( Class<? extends NGDynamicElement> elementClass, List<String> tagNames, List<NGBindingDescription> bindings, String text ) {

	public record NGBindingDescription( String name, String text ) {

	}
}