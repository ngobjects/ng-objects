package ng.appserver.templating;

import ng.appserver.NGContext;
import ng.appserver.templating.assications.NGAssociation;

/**
 * Utility methods for dynamc elements, mostly input elements.
 *
 * CHECKME: This might have to end up as a superclass instead of a utility class
 */

public class NGDynamicElementUtils {

	/**
	 * @return The name of the field (to use in the HTML code)
	 */
	public static String name( final NGAssociation association, final NGContext context ) {

		if( association != null ) {
			return (String)association.valueInComponent( context.component() );
		}

		return context.elementID().toString();
	}
}