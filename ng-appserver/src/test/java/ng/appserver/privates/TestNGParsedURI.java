package ng.appserver.privates;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

public class TestNGParsedURI {

	@Test
	public void testOf() {
		NGParsedURI parsedURI = NGParsedURI.of( "/" );
		assertTrue( parsedURI.elementAt( 0 ).isEmpty() );
	}
}