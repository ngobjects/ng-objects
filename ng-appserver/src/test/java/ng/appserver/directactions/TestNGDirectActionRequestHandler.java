package ng.appserver.directactions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.NGActionResults;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGResponse;
import ng.appserver.http.NGResponses;
import ng.appserver.http.NGStandardRequest;

public class TestNGDirectActionRequestHandler {

	@Test
	public void anUnregisteredClassIsNotFound() {
		final NGResponse response = handle( "/na/NoSuchDirectActionClass/someAction" );
		assertEquals( 404, response.status() );
		assertEquals( "text/plain; charset=utf-8", response.headerForKey( "content-type" ) );
	}

	@Test
	public void aMissingMethodIsNotFound() {
		NGDirectActionRequestHandler.registerDirectActionClass( TestActions.class );
		final NGResponse response = handle( "/na/TestActions/noSuchMethod" );
		assertEquals( 404, response.status() );
		assertEquals( "text/plain; charset=utf-8", response.headerForKey( "content-type" ) );
	}

	@Test
	public void aRegisteredActionIsPerformed() {
		NGDirectActionRequestHandler.registerDirectActionClass( TestActions.class );
		assertEquals( 200, handle( "/na/TestActions/hello" ).status() );
	}

	private static NGResponse handle( final String uri ) {
		final NGRequest request = new NGStandardRequest( "GET", uri, null, "HTTP/1.1", Map.of(), InputStream.nullInputStream(), "127.0.0.1" );
		return new NGDirectActionRequestHandler().handleRequest( request );
	}

	public static class TestActions extends NGDirectAction {

		public TestActions( final NGRequest request ) {
			super( request );
		}

		public NGActionResults helloAction() {
			return NGResponses.ok( "hello" );
		}
	}
}
