package ng.appserver.privates;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestNGParsedURI {

	@Test
	public void testOfEmptyURI() {
		NGParsedURI parsedURI = NGParsedURI.of( "/" );
		assertNull( parsedURI.getString( 0 ) );
	}

	@Test
	public void testOf() {
		NGParsedURI parsedURI = NGParsedURI.of( "/smu/bla/" );
		assertTrue( parsedURI.getString( 0 ).equals( "smu" ) );
		assertTrue( parsedURI.getString( 1 ).equals( "bla" ) );
		assertNull( parsedURI.getString( 2 ) );
	}

	@Test
	public void testOfFailsOnNull() {
		assertThrows( NullPointerException.class, () -> {
			NGParsedURI.of( null );
		} );
	}
}