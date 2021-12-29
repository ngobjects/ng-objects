package ng.appserver.experimental;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGRequest;
import ng.appserver.privates.NGParsedURI;

/**
 * Contains a list of handlers for URLs
 *
 * Routing allows a couple of types of wildcards
 *
 * - Named parameters are prefixed with a colon
 * - Positional parameters are a star
 */

public class NGRouteTable {

	/**
	 * A list of all routes mapped by this table
	 */
	private List<Route> _routes = new ArrayList<>();

	/**
	 * The default global route table used by RouteAction to access actions
	 */
	private static NGRouteTable _defaultRouteTable = new NGRouteTable();

	public static NGRouteTable defaultRouteTable() {
		return _defaultRouteTable;
	}

	private List<Route> routes() {
		return _routes;
	}

	public NGRouteHandler handlerForURL( final NGParsedURI url ) {

		for( final Route route : routes() ) {
			if( matches( route.pattern, url.sourceURL() ) ) {
				return route.routeHandler;
			}
		}

		return null;
	}

	/**
	 * Check if the given handler matches the given URL
	 */
	private static boolean matches( final String pattern, final String url ) {
		return url.startsWith( pattern );
	}

	public void map( final String pattern, final NGRouteHandler routeHandler ) {
		Route r = new Route();
		r.pattern = pattern;
		r.routeHandler = routeHandler;
		_routes.add( r );
	}

	public void map( final String pattern, final Function<NGRequest, NGActionResults> function ) {
		final FunctionRouteHandler routeHandler = new FunctionRouteHandler( function );
		map( pattern, routeHandler );
	}

	public void mapComponent( final String pattern, final Class<? extends NGComponent> componentClass ) {
		final ComponentRouteHandler routeHandler = new ComponentRouteHandler( componentClass );
		map( pattern, routeHandler );
	}

	/**
	 * Maps a URL pattern to a given RouteHandler
	 */
	public static class Route {

		/**
		 * The pattern this route uses
		 */
		public String pattern;

		/**
		 * The routeHandler that will handle requests passed to this route
		 */
		public NGRouteHandler routeHandler;
	}

	public static abstract class NGRouteHandler {
		public abstract NGActionResults handleRequest( NGRequest request );
	}

	public static class FunctionRouteHandler extends NGRouteHandler {
		private Function<NGRequest, NGActionResults> _function;

		public FunctionRouteHandler( final Function<NGRequest, NGActionResults> biFunction ) {
			_function = biFunction;
		}

		@Override
		public NGActionResults handleRequest( NGRequest request ) {
			return _function.apply( request );
		}
	}

	public static class ComponentRouteHandler extends NGRouteHandler {
		private Class<? extends NGComponent> _componentClass;

		public ComponentRouteHandler( final Class<? extends NGComponent> componentClass ) {
			_componentClass = componentClass;
		}

		@Override
		public NGActionResults handleRequest( NGRequest request ) {
			return NGApplication.application().pageWithName( _componentClass, request.context() );
		}
	}
}