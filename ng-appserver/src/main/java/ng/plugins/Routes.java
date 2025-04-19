package ng.plugins;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ng.appserver.NGActionResults;
import ng.appserver.NGRequest;
import ng.appserver.NGRequestHandler;
import ng.appserver.routing.NGRouteTable;
import ng.appserver.routing.NGRouteTable.Route;
import ng.appserver.templating.NGComponent;

/**
 * Functions basically as a builder for a RouteTable
 */

public class Routes {

	/**
	 * CHECKME: We're using an NGRouteTable to keep track of registered routes, only to get access to it's existing route construction API
	 */
	private NGRouteTable _routeTable = new NGRouteTable();

	/**
	 * Instances are created using the create() method
	 */
	private Routes() {}

	/**
	 * @return A new instance
	 */
	public static Routes create() {
		return new Routes();
	}

	public Routes map( String pattern, Class<? extends NGComponent> componentClass ) {
		_routeTable.map( pattern, componentClass );
		return this;
	}

	public Routes map( final String pattern, final NGRequestHandler requestHandler ) {
		_routeTable.map( pattern, requestHandler );
		return this;
	}

	public Routes map( final String pattern, final Function<NGRequest, NGActionResults> function ) {
		_routeTable.map( pattern, function );
		return this;
	}

	public Routes map( final String pattern, final Supplier<NGActionResults> supplier ) {
		_routeTable.map( pattern, supplier );
		return this;
	}

	public List<Route> routes() {
		return _routeTable.routes();
	}
}