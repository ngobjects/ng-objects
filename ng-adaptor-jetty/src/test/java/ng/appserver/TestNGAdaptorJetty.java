package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TestNGAdaptorJetty {

	@Test
	public void testStatus() throws IOException, InterruptedException {
		NGApplication.run( new String[0], SmuApplication.class );

		final HttpClient client = HttpClient.newHttpClient();
		final HttpRequest request = HttpRequest
				.newBuilder()
				.uri( URI.create( "http://localhost:1200/some-route/" ) )
				.build();

		HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );
		assertEquals( "Oh look, a 404 response!", response.body() );
		assertEquals( 404, response.statusCode() );
		assertEquals( List.of( "firstValue", "secondValue" ), response.headers().allValues( "someHeader" ) );
	}

	/**
	 * Application implementation to test the features of the Jetty Adaptor
	 */
	public static class SmuApplication extends NGApplication {

		public SmuApplication() {
			routeTable().map( "/some-route/", ( request ) -> {
				final NGResponse response = new NGResponse( "Oh look, a 404 response!", 404 );
				response.setHeader( "someHeader", "firstValue" );
				response.setHeader( "someHeader", "secondValue" );
				// response.addCookie( ... ) // FIXME: Implement test once implemented
				return response;
			} );
		}
	}
}