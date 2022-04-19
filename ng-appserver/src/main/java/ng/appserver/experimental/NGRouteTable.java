package ng.appserver.experimental;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGRequest;
import ng.appserver.NGRequestHandler;
import ng.appserver.NGResponse;

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
	 * @return All registered routes in the table
	 */
	public List<Route> routes() {
		return _routes;
	}

	public NGRequestHandler handlerForURL( final String uri ) {
		Objects.requireNonNull( uri );

		for( final Route route : routes() ) {
			if( matches( route.pattern(), uri ) ) {
				return route.routeHandler();
			}
		}

		return null;
	}

	/**
	 * Check if the given handler matches the given URL.
	 *
	 * FIXME: We're currently only checking if the pattern starts with the given pattern. We want some real pattern matching here // Hugi 2021-12-30
	 */
	private static boolean matches( final String pattern, final String uri ) {
		return uri.startsWith( pattern );
	}

	public void map( final String pattern, final NGRequestHandler requestHandler ) {
		Route r = new Route();
		r._pattern = pattern;
		r._routeHandler = requestHandler;
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
		private String _pattern;

		/**
		 * The routeHandler that will handle requests passed to this route
		 */
		private NGRequestHandler _routeHandler;

		public String pattern() {
			return _pattern;
		}

		public NGRequestHandler routeHandler() {
			return _routeHandler;
		}
	}

	public static class FunctionRouteHandler extends NGRequestHandler {
		private Function<NGRequest, NGActionResults> _function;

		public FunctionRouteHandler( final Function<NGRequest, NGActionResults> biFunction ) {
			_function = biFunction;
		}

		@Override
		public NGResponse handleRequest( NGRequest request ) {
			return _function.apply( request ).generateResponse();
		}
	}

	public static class ComponentRouteHandler extends NGRequestHandler {
		private Class<? extends NGComponent> _componentClass;

		public ComponentRouteHandler( final Class<? extends NGComponent> componentClass ) {
			_componentClass = componentClass;
		}

		@Override
		public NGResponse handleRequest( NGRequest request ) {
			return NGApplication.application().pageWithName( _componentClass, request.context() ).generateResponse();
		}
	}
}