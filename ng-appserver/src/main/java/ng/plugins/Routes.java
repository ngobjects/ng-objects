package ng.plugins;

import java.util.function.Function;
import java.util.function.Supplier;

import ng.appserver.NGActionResults;
import ng.appserver.NGRequest;
import ng.appserver.NGRequestHandler;
import ng.appserver.routing.NGRouteTable;
import ng.appserver.templating.NGComponent;

/**
 * Functions basically as a builder for a RouteTable
 */

public class Routes {

	private NGRouteTable _routeTable = new NGRouteTable();

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

	public NGRouteTable routeTable() {
		return _routeTable;
	}

	private Routes() {}

	public static Routes create() {
		return new Routes();
	}
}