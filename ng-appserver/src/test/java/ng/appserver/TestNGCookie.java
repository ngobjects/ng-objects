package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

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
		final NGCookie cookie = new NGCookie( "someName", "someValue", Duration.ofHours( 1 ) );
		assertEquals( Long.valueOf( 3600L ), cookie.maxAge() );
	}

	@Test
	public void nullDurationMeansSessionCookie() {
		final NGCookie cookie = new NGCookie( "someName", "someValue", (Duration)null );
		assertNull( cookie.maxAge() );
	}

	@Test
	public void zeroDurationMeansDeletion() {
		final NGCookie cookie = new NGCookie( "someName", "someValue", Duration.ZERO );
		assertEquals( Long.valueOf( 0L ), cookie.maxAge() );
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

	/**
	 * On the wire, negative Max-Age means "expire now" — but the adaptors' cookie APIs use negative as their
	 * "no Max-Age" sentinel (a session cookie), the opposite meaning. Construction rejects the ambiguity.
	 */
	@Test
	public void negativeMaxAgeIsRejected() {
		assertThrows( IllegalArgumentException.class, () -> new NGCookie( "someName", "someValue", Duration.ofSeconds( -1 ) ) );
		assertThrows( IllegalArgumentException.class, () -> new NGCookie( "someName", "someValue", null, "/", -1L, false, true, "Lax" ) );
	}
}
