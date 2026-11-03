package ng.appserver.templating;

import java.util.Map;

import ng.appserver.templating.associations.NGAssociation;

/**
 * Common superclass for all dynamic elements.
 *
 * Probably could be an interface, but implemented as an abstract class to enforce usage of the constructor.
 */

public abstract class NGDynamicElement implements NGElement {

	/**
	 * The constructor all dynamic elements must use, passing on the arguments they were constructed with
	 *
	 * @param name The element's name, as used in the template
	 * @param associations The element's bindings
	 * @param contentTemplate The template the element wraps. null if it has no content.
	 */
	public NGDynamicElement( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {}
}