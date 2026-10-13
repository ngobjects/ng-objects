package ng.plugins;

import ng.appserver.NGApplication;
import ng.appserver.NGActionResults;
import ng.appserver.http.NGResponse;
import ng.appserver.http.NGResponses;
import ng.appserver.NGComponentRequestHandler;
import ng.appserver.NGResourceRequestHandler;
import ng.appserver.NGResourceRequestHandlerDynamic;
import ng.appserver.directactions.NGDirectActionRequestHandler;

public class NGCorePlugin implements NGPlugin {

	@Override
	public String namespace() {
		return "ng";
	}

	@Override
	public Routes routes() {
		return Routes
				.create()
				.map( "/", request -> NGApplication.application().defaultResponse( request ) )
				.map( NGComponentRequestHandler.DEFAULT_PATH + "*", new NGComponentRequestHandler() )
				.map( NGResourceRequestHandler.DEFAULT_PATH + "*", new NGResourceRequestHandler() )
				.map( NGResourceRequestHandlerDynamic.DEFAULT_PATH + "*", new NGResourceRequestHandlerDynamic() )
				.map( NGDirectActionRequestHandler.DEFAULT_PATH + "*", new NGDirectActionRequestHandler() )
				.map( "/ng/sessionCookieReset", request -> NGApplication.application().resetSessionCookie() )
				.map( "/ng/describe", request -> describe() );
	}

	/**
	 * Identifies the framework to management tools - wotaskd probes this to type an instance
	 * (see wonder-slim-deployment#56).
	 *
	 * FIXME: Placeholder awaiting the proper describe protocol, shared with wonder-slim - framework name
	 * plus the running version and other environment metadata. For now: the framework name only // Hugi 2026-09-22
	 */
	private static NGActionResults describe() {
		final NGResponse response = NGResponses.ok( "{\"framework\":\"ng-objects\"}" );
		response.setHeader( "content-type", "application/json" );
		return response;
	}
}