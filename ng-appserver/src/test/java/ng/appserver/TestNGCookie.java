package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGCookie;

public class TestNGCookie {

	@Test
	public void constructSimpleCookie() {
		final NGCookie cookie = new NGCookie( "someName", "someValue" );
		assertEquals( "someName", cookie.name() );
		assertEquals( "someValue", cookie.value() );
	}

	@Test
	public void constructWithMaxAge() {
		final NGCookie cookie = new NGCookie( "someName", "someValue", 3600L );
		assertEquals( Long.valueOf( 3600L ), cookie.maxAge() );
	}

	/**
	 * The convenience constructors' defaults are API contract — apps build on them invisibly, so they must not drift
	 */
	@Test
	public void convenienceConstructorDefaults() {
		final NGCookie cookie = new NGCookie( "someName", "someValue" );

		assertEquals( "/", cookie.path() );
		assertNull( cookie.domain() );
		assertNull( cookie.maxAge() );
		assertTrue( cookie.httpOnly() );
		assertFalse( cookie.secure() );
		assertEquals( "Lax", cookie.sameSite() );
	}

	@Test
	public void nameAndValueAreRequired() {
		assertThrows( NullPointerException.class, () -> new NGCookie( null, "someValue" ) );
		assertThrows( NullPointerException.class, () -> new NGCookie( "someName", null ) );
	}

	/**
	 * Browsers reject SameSite=None cookies that aren't Secure, so construction does too
	 */
	@Test
	public void sameSiteNoneRequiresSecure() {
		assertThrows( IllegalArgumentException.class, () -> new NGCookie( "someName", "someValue", null, "/", null, false, true, "None" ) );

		// The same combination with secure=true is legal
		final NGCookie cookie = new NGCookie( "someName", "someValue", null, "/", null, true, true, "None" );
		assertEquals( "None", cookie.sameSite() );
		assertTrue( cookie.secure() );
	}
}
