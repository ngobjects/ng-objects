package ng.appserver.templating.elements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.NGComponentRequestTestHarness.TestContext;
import ng.appserver.NGComponentRequestTestHarness.TestPage;
import ng.appserver.NGComponentRequestTestHarness.TestRequest;
import ng.appserver.NGContext;
import ng.appserver.NGSession;
import ng.appserver.http.NGResponses;
import ng.appserver.http.NGStandardResponse;
import ng.appserver.templating.NGComponent;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.associations.NGConstantValueAssociation;

public class TestNGPopUpButton {

	private static final List<String> LIST = List.of( "a", "b", "c" );

	@Test
	public void singleSelection() {
		final Holder selection = new Holder();
		takeValues( popUp( "selection", selection ), "1" );
		assertEquals( "b", selection.value );
	}

	@Test
	public void singleNoSelection() {
		final Holder selection = new Holder();
		selection.value = "a";
		takeValues( popUp( "selection", selection ), "NO_SELECTION_OPTION_VALUE" );
		assertNull( selection.value );
	}

	@Test
	public void singleEmptyValueFails() {
		final IllegalStateException e = assertThrows( IllegalStateException.class, () -> takeValues( popUp( "selection", new Holder() ), "" ) );
		assertTrue( e.getMessage().contains( "''" ), e.getMessage() );
		assertTrue( e.getMessage().contains( "'pop'" ), e.getMessage() );
	}

	@Test
	public void singleNonNumericValueFails() {
		assertThrows( IllegalStateException.class, () -> takeValues( popUp( "selection", new Holder() ), "x" ) );
	}

	@Test
	public void singleOutOfRangeIndexFails() {
		assertThrows( IllegalStateException.class, () -> takeValues( popUp( "selection", new Holder() ), "3" ) );
	}

	@Test
	public void multipleSelection() {
		final Holder selections = new Holder();
		takeValues( popUp( "selections", selections, "multiple", true ), "0", "2" );
		assertEquals( List.of( "a", "c" ), selections.value );
	}

	@Test
	public void multipleOutOfRangeIndexFails() {
		assertThrows( IllegalStateException.class, () -> takeValues( popUp( "selections", new Holder(), "multiple", true ), "0", "5" ) );
	}

	@Test
	public void multipleEmptyValueFails() {
		assertThrows( IllegalStateException.class, () -> takeValues( popUp( "selections", new Holder(), "multiple", true ), "" ) );
	}

	@Test
	public void renderWithoutSelectionBindingMarksSubmittedOption() {
		final String html = render( popUp(), "1" );
		assertTrue( html.contains( "<option value=\"1\" selected>b</option>" ), html );
	}

	@Test
	public void renderWithoutSelectionBindingIgnoresMalformedValue() {
		final String html = render( popUp(), "x" );
		assertFalse( html.contains( "selected" ), html );
	}

	/**
	 * An association that stores whatever value is set on it
	 */
	private static class Holder extends NGAssociation {

		Object value;

		@Override
		public Object valueInComponent( NGComponent component ) {
			return value;
		}

		@Override
		public void setValue( Object value, NGComponent component ) {
			this.value = value;
		}
	}

	/**
	 * @return A pop-up button named 'pop' over LIST, with the given additional bindings (constant values unless an NGAssociation is given)
	 */
	private static NGPopUpButton popUp( final Object... namesAndValues ) {
		final Map<String, NGAssociation> associations = new HashMap<>();
		associations.put( "list", new NGConstantValueAssociation( LIST ) );
		associations.put( "name", new NGConstantValueAssociation( "pop" ) );

		for( int i = 0; i < namesAndValues.length; i += 2 ) {
			final Object value = namesAndValues[i + 1];
			associations.put( (String)namesAndValues[i], value instanceof NGAssociation a ? a : new NGConstantValueAssociation( value ) );
		}

		return new NGPopUpButton( null, associations, null );
	}

	/**
	 * @return A context for a request submitting the given values for 'pop', with a page as the current component
	 */
	private static NGContext context( final String... submittedValues ) {
		final TestRequest request = new TestRequest( "/no/1.0", Map.of( "pop", List.of( submittedValues ) ) );
		final NGContext context = new TestContext( request, new NGSession() );
		context.setComponent( new TestPage( context, "" ) );
		return context;
	}

	private static void takeValues( final NGPopUpButton popUp, final String... submittedValues ) {
		final NGContext context = context( submittedValues );
		popUp.takeValuesFromRequest( context.request(), context );
	}

	private static String render( final NGPopUpButton popUp, final String... submittedValues ) {
		final NGContext context = context( submittedValues );
		final NGStandardResponse response = (NGStandardResponse)NGResponses.of();
		popUp.appendToResponse( response, context );
		return response.contentString();
	}
}
