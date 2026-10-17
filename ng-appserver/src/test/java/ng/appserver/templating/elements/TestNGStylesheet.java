package ng.appserver.templating.elements;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.NGComponentRequestTestHarness.TestContext;
import ng.appserver.NGComponentRequestTestHarness.TestPage;
import ng.appserver.NGComponentRequestTestHarness.TestRequest;
import ng.appserver.NGContext;
import ng.appserver.NGSession;
import ng.appserver.http.NGResponses;
import ng.appserver.http.NGStandardResponse;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.associations.NGConstantValueAssociation;

public class TestNGStylesheet {

	@Test
	public void rendersTheResourceURL() {
		assertEquals( "<link rel=\"stylesheet\" type=\"text/css\" href=\"/nr/app/css/app.css\"/>", render( "namespace", "app" ) );
	}

	@Test
	public void passesOtherBindingsThroughAsAttributes() {
		assertEquals( "<link rel=\"stylesheet\" type=\"text/css\" href=\"/nr/app/css/app.css\" media=\"print\" id=\"main\"/>", render( "namespace", "app", "media", "print", "id", "main" ) );
	}

	@Test
	public void legacyFrameworkBindingIsNotPassedThrough() {
		assertEquals( "<link rel=\"stylesheet\" type=\"text/css\" href=\"/nr/app/css/app.css\"/>", render( "framework", "app" ) );
	}

	/**
	 * @return The rendered element for 'css/app.css', with the given bindings as constant values, in order
	 */
	private static String render( final String... namesAndValues ) {
		final Map<String, NGAssociation> associations = new LinkedHashMap<>();
		associations.put( "filename", new NGConstantValueAssociation( "css/app.css" ) );

		for( int i = 0; i < namesAndValues.length; i += 2 ) {
			associations.put( namesAndValues[i], new NGConstantValueAssociation( namesAndValues[i + 1] ) );
		}

		final NGStylesheet element = new NGStylesheet( null, associations, null );

		final NGContext context = new TestContext( new TestRequest( "/no/1.0", Map.of() ), new NGSession() );
		context.setComponent( new TestPage( context, "" ) );

		final NGStandardResponse response = (NGStandardResponse)NGResponses.of();
		element.appendToResponse( response, context );
		return response.contentString();
	}
}
