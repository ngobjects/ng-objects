package ng.appserver.templating.elements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import ng.appserver.NGComponentRequestTestHarness;
import ng.appserver.NGComponentRequestTestHarness.TestPage;
import ng.appserver.NGContext;
import ng.appserver.http.NGResponses;
import ng.appserver.http.NGStandardResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.associations.NGConstantValueAssociation;

/**
 * Values that reach the rendered page through bindings are rendered as text, never as markup
 */
public class TestNGElementEscaping {

	private static final String HOSTILE = "\" onclick=\"alert('x')\"><script>";

	private static final String HOSTILE_ESCAPED = "&quot; onclick=&quot;alert(&#39;x&#39;)&quot;&gt;&lt;script&gt;";

	@Test
	public void createElementStringWithAttributes() {
		assertEquals( "<a title=\"" + HOSTILE_ESCAPED + "\">", NGHTMLUtilities.createElementStringWithAttributes( "a", Map.of( "title", HOSTILE ), false ) );
		assertEquals( "<a title=\"Tom &amp; Jerry\">", NGHTMLUtilities.createElementStringWithAttributes( "a", Map.of( "title", "Tom & Jerry" ), false ) );
	}

	@Test
	public void genericElementAttributes() {
		final String html = render( new NGGenericElement( null, associations( "elementName", "img", "alt", HOSTILE ), null ) );
		assertTrue( html.contains( "alt=\"" + HOSTILE_ESCAPED + "\"" ), html );
		assertFalse( html.contains( "<script>" ), html );
	}

	@Test
	public void genericContainerAttributes() {
		final String html = render( new NGGenericContainer( null, associations( "elementName", "div", "title", HOSTILE ), NGHTMLBareString.EMPTY ) );
		assertTrue( html.contains( "title=\"" + HOSTILE_ESCAPED + "\"" ), html );
		assertFalse( html.contains( "<script>" ), html );
	}

	@Test
	public void popUpButtonOptionText() {
		final String html = render( new NGPopUpButton( null, associations( "list", List.of( HOSTILE ), "noSelectionString", HOSTILE ), null ) );
		assertEquals( 2, html.split( Pattern.quote( HOSTILE_ESCAPED ), -1 ).length - 1, html );
		assertFalse( html.contains( "<script>" ), html );
	}

	@Test
	public void fileUploadName() {
		final String html = render( new NGFileUpload( null, associations( "name", HOSTILE ), null ) );
		assertEquals( "<input type=\"file\" name=\"" + HOSTILE_ESCAPED + "\"/>", html );
	}

	/**
	 * @return Constant-value associations for the given name/value pairs
	 */
	private static Map<String, NGAssociation> associations( final Object... namesAndValues ) {
		final Map<String, NGAssociation> associations = new HashMap<>();

		for( int i = 0; i < namesAndValues.length; i += 2 ) {
			associations.put( (String)namesAndValues[i], new NGConstantValueAssociation( namesAndValues[i + 1] ) );
		}

		return associations;
	}

	/**
	 * @return The HTML rendered by [element] in a fresh context
	 */
	private static String render( final NGElement element ) {
		final TestPage page = NGComponentRequestTestHarness.newPage( "" );
		final NGContext context = page.context();
		context.setComponent( page );

		final NGStandardResponse response = (NGStandardResponse)NGResponses.of();
		element.appendToResponse( response, context );
		return response.contentString();
	}
}
