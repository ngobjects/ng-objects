package ng.appserver.routing;

import java.util.ArrayList;
import java.util.List;

import ng.appserver.NGRequestHandler;

/**
 * Stores information about our registered routes, and decides how to handle a request based on those tables.
 */

public class NGRouteManager {

	/**
	 * Our route tables
	 */
	private List<NGRouteTable> _routeTables = new ArrayList<>();

	/**
	 * @return a request handler for the given URL, by searching all route tables
	 */
	public NGRequestHandler handlerForURL( String url ) {
		for( final NGRouteTable routeTable : _routeTables ) {
			final NGRequestHandler handler = routeTable.handlerForURL( url );

			if( handler != null ) {
				return handler;
			}
		}

		return null;
	}

	/**
	 * Our route tables
	 */
	public List<NGRouteTable> routeTables() {
		return _routeTables;
	}
}