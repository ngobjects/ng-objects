package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestNGCookie {

	@Test
	public void constructSimpleCookie() {
		NGCookie cookie = new NGCookie( "someName", "someValue" );
		assertEquals( "someName", cookie.name() );
		assertEquals( "someValue", cookie.value() );
	}
}
