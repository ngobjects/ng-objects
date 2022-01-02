package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TestNGAdaptorJetty {

	@Test
	public void testRequestHandling() throws IOException, InterruptedException {
		NGApplication.run( new String[0], SmuApplication.class );

		final HttpClient client = HttpClient
				.newBuilder()
				.build();

		final HttpRequest request = HttpRequest
				.newBuilder()
				.uri( URI.create( "http://localhost:1200/first/second?formKey=formValue" ) )
				.header( "someRequestHeader", "someRequestHeaderValue1" )
				.header( "someRequestHeader", "someRequestHeaderValue2" )
				.build();

		HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );

		// Check the response values as seen by the HTTP client
		assertEquals( 404, response.statusCode() );
		assertEquals( List.of( "firstValue", "secondValue" ), response.headers().allValues( "someHeader" ) );
		assertEquals( List.of( "someCookieName=someCookieValue" ), response.headers().allValues( "set-cookie" ) );
		assertEquals( "Oh look, a 404 response!", response.body() );

		// Check the request values as seen by the application class
		final NGRequest lsr = ((SmuApplication)NGApplication.application()).lastServedRequest;
		assertEquals( "/first/second", lsr.uri() );
		assertEquals( Map.of( "formKey", List.of( "formValue" ) ), lsr.formValues() );
		assertEquals( List.of( "someRequestHeaderValue1", "someRequestHeaderValue2" ), lsr.headers().get( "someRequestHeader" ) );
	}

	/**
	 * Application implementation to test the features of the Jetty Adaptor
	 */
	public static class SmuApplication extends NGApplication {

		/**
		 * Keep track of the last served request so we can check how it looked
		 */
		public NGRequest lastServedRequest;

		public SmuApplication() {
			routeTable().map( "/first/second", ( request ) -> {
				lastServedRequest = request;

				final NGResponse response = new NGResponse( "Oh look, a 404 response!", 404 );
				response.setHeader( "someHeader", "firstValue" );
				response.setHeader( "someHeader", "secondValue" );
				response.addCookie( new NGCookie( "someCookieName", "someCookieValue" ) );
				return response;
			} );
		}
	}
}