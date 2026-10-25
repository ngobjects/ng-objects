package ng.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGStandardRequest;
import ng.appserver.routing.NGRouteTable.Route;

public class TestNGDevelopmentPlugin {

	@Test
	public void everyEndpointRefusesARemoteClient() {
		final List<Route> routes = new NGDevelopmentPlugin().routes().routes();
		assertFalse( routes.isEmpty() );

		for( final Route route : routes ) {
			final NGStandardRequest request = new NGStandardRequest( "GET", route.pattern(), null, "HTTP/1.1", Map.of(), InputStream.nullInputStream(), "203.0.113.7" );
			assertEquals( 403, route.handler().handleRequest( request ).status(), route.pattern() );
		}
	}

	@Test
	public void aLoopbackClientIsAnswered() {
		final Route type = new NGDevelopmentPlugin().routes().routes().stream().filter( route -> route.pattern().equals( "/ng/dev/type" ) ).findFirst().orElseThrow();
		final NGStandardRequest request = new NGStandardRequest( "GET", "/ng/dev/type", null, "HTTP/1.1", Map.of(), InputStream.nullInputStream(), "127.0.0.1" );
		assertEquals( 200, type.handler().handleRequest( request ).status() );
	}
}
