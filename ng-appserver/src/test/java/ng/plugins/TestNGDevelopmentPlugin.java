package ng.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGRequest;
import ng.appserver.http.NGRequests;
import ng.appserver.routing.NGRouteTable.Route;

public class TestNGDevelopmentPlugin {

	@Test
	public void everyEndpointRefusesARemoteClient() {
		final List<Route> routes = new NGDevelopmentPlugin().routes().routes();
		assertFalse( routes.isEmpty() );

		for( final Route route : routes ) {
			final NGRequest request = NGRequests.of( "GET", route.pattern(), null, Map.of(), InputStream.nullInputStream(), "203.0.113.7", null );
			assertEquals( 403, route.handler().handleRequest( request ).status(), route.pattern() );
		}
	}

	@Test
	public void aLoopbackClientIsAnswered() {
		final Route type = new NGDevelopmentPlugin().routes().routes().stream().filter( route -> route.pattern().equals( "/ng/dev/type" ) ).findFirst().orElseThrow();
		final NGRequest request = NGRequests.of( "GET", "/ng/dev/type", null, Map.of(), InputStream.nullInputStream(), "127.0.0.1", null );
		assertEquals( 200, type.handler().handleRequest( request ).status() );
	}
}
