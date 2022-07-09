package ng.appserver;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.elements.docs.NGDynamicElementDescription;

/**
 * Common superclass for all dynamic elements.
 *
 * Probably could be an interface, but implemented as an abstract class to enforce usage of the constructor.
 */

public abstract class NGDynamicElement extends NGElement {

	private static final Logger logger = LoggerFactory.getLogger( NGDynamicElement.class );

	/**
	 * The constructor all dynamic elements must use. In the case of this class, it's a no-op so for sanity, we ensure we're always passing in null for all arguments
	 */
	public NGDynamicElement( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {

		if( name != null || associations != null || template != null ) {
			logger.warn( "name, associations or template was not null. This constructor should only be invoked with null parameters" );
			// FIXME: Since this is a weird state, we should probably be throwing here instead of logging. Babysteps. // Hugi 2022-07-09
			// throw new IllegalArgumentException( "name, associations or template was not null. This constructor should only be invoked with null parameters" );
		}
	}

	/**
	 * @return The API description of this element
	 *
	 * FIXME: The description class might perhaps be better provided by an interface that can be optionally implemented by any classes that extend WOElement (including components)
	 */
	public NGDynamicElementDescription dynamicElementDescription() {
		return NGDynamicElementDescription.NoDescription;
	}
}