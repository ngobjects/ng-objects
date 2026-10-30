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
	 * The constructor all dynamic elements must use. In the case of this class, it's a no-op so for sanity, we ensure we're always passing in null for all arguments
	 */
	public NGDynamicElement( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {}
}