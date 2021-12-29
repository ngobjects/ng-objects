package ng.appserver.privates;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TestNGParsedURI {

	@Test
	public void testOf() {
		NGParsedURI parsedURI = NGParsedURI.of( "/" );
		assertTrue( parsedURI.elementAt( 0 ).isEmpty() );
	}

	@Test
	public void testOfFailsOnNull() {
		assertThrows( NullPointerException.class, () -> {
			NGParsedURI.of( null );
		} );
	}
}