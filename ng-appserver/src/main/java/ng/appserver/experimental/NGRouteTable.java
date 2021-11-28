package ng.appserver.experimental;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
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

	private static final NotFoundRouteHandler NOT_FOUND_ROUTE_HANDLER = new NotFoundRouteHandler();

	private static final Logger logger = LoggerFactory.getLogger( NGRouteTable.class );

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

	public NGRouteHandler handlerForURL( final WrappedURL url ) {

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

	/**
	 * Handle the given URL
	 */
	public NGActionResults handle( final WrappedURL url, final NGContext context ) {
		final NGRequest request = context.request();
		logger.info( "Handling URL: {}", url );
		NGRouteHandler routeHandler = handlerForURL( url );

		if( routeHandler == null ) {
			logger.warn( "No RouteHandler found for URL: {}", url.toString() );
			routeHandler = NOT_FOUND_ROUTE_HANDLER;
		}

		return routeHandler.handle( url, context );
	}

	public void map( final String pattern, final NGRouteHandler routeHandler ) {
		Route r = new Route();
		r.pattern = pattern;
		r.routeHandler = routeHandler;
		_routes.add( r );
	}

	public void map( final String pattern, final BiFunction<WrappedURL, NGContext, NGActionResults> biFunction ) {
		final BiFunctionRouteHandler routeHandler = new BiFunctionRouteHandler( biFunction );
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
		public abstract NGActionResults handle( WrappedURL url, NGContext context );
	}

	/**
	 * For returning 404
	 */
	public static class NotFoundRouteHandler extends NGRouteHandler {
		@Override
		public NGActionResults handle( final WrappedURL url, NGContext context ) {
			final NGResponse response = new NGResponse();
			response.setStatus( 404 );
			response.setContentString( "Not found: " + url );
			return response;
		}
	}

	public static class BiFunctionRouteHandler extends NGRouteHandler {
		private BiFunction<WrappedURL, NGContext, NGActionResults> _biFunction;

		public BiFunctionRouteHandler( final BiFunction<WrappedURL, NGContext, NGActionResults> biFunction ) {
			_biFunction = biFunction;
		}

		@Override
		public NGActionResults handle( WrappedURL url, NGContext context ) {
			return _biFunction.apply( url, context );
		}
	}

	public static class ComponentRouteHandler extends NGRouteHandler {
		private Class<? extends NGComponent> _componentClass;

		public ComponentRouteHandler( final Class<? extends NGComponent> componentClass ) {
			_componentClass = componentClass;
		}

		@Override
		public NGActionResults handle( WrappedURL url, NGContext context ) {
			return NGApplication.application().pageWithName( _componentClass, context );
		}
	}
}