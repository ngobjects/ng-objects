package ng.appserver.privates;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TestNGParsedURI {

	@Test
	public void testOfEmptyURI() {
		NGParsedURI parsedURI = NGParsedURI.of( "/" );
		assertTrue( parsedURI.elementAt( 0 ).isEmpty() );
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