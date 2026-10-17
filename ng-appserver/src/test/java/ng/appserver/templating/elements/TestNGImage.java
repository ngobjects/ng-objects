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

public class TestNGImage {

	@Test
	public void namespaceIsNotPassedThrough() {
		assertEquals( "<img src=\"/nr/app/img/logo.png\"/>", render( "namespace", "app" ) );
	}

	@Test
	public void legacyFrameworkBindingIsNotPassedThrough() {
		assertEquals( "<img src=\"/nr/app/img/logo.png\"/>", render( "framework", "app" ) );
	}

	@Test
	public void namespaceWinsOverFrameworkAndNeitherIsPassedThrough() {
		assertEquals( "<img src=\"/nr/app/img/logo.png\"/>", render( "namespace", "app", "framework", "other" ) );
	}

	@Test
	public void passesOtherBindingsThroughAsAttributes() {
		assertEquals( "<img src=\"/nr/app/img/logo.png\" alt=\"Logo\"/>", render( "namespace", "app", "alt", "Logo" ) );
	}

	/**
	 * @return The rendered element for 'img/logo.png', with the given bindings as constant values
	 */
	private static String render( final String... namesAndValues ) {
		final Map<String, NGAssociation> associations = new LinkedHashMap<>();
		associations.put( "filename", new NGConstantValueAssociation( "img/logo.png" ) );

		for( int i = 0; i < namesAndValues.length; i += 2 ) {
			associations.put( namesAndValues[i], new NGConstantValueAssociation( namesAndValues[i + 1] ) );
		}

		final NGImage element = new NGImage( null, associations, null );

		final NGContext context = new TestContext( new TestRequest( "/no/1.0", Map.of() ), new NGSession() );
		context.setComponent( new TestPage( context, "" ) );

		final NGStandardResponse response = (NGStandardResponse)NGResponses.of();
		element.appendToResponse( response, context );
		return response.contentString();
	}
}
