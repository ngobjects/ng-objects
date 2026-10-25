package ng.adaptor.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGCookie;
import ng.appserver.http.NGCookie.SameSite;

public class TestNGAdaptorJava {

	@Test
	public void setCookieHeader() {
		assertEquals( "ngsid=abc; Path=/; Max-Age=3600; HttpOnly; SameSite=Lax", NGAdaptorJava.setCookieHeaderValue( new NGCookie( "ngsid", "abc", Duration.ofHours( 1 ) ) ) );
	}

	@Test
	public void setCookieHeaderWithEverything() {
		assertEquals( "a=1; Domain=example.com; Path=/app; Max-Age=0; Secure; HttpOnly; SameSite=Strict", NGAdaptorJava.setCookieHeaderValue( new NGCookie( "a", "1", "example.com", "/app", 0L, true, true, SameSite.STRICT ) ) );
	}

	@Test
	public void sessionCookieHasNoMaxAge() {
		assertEquals( "a=1; Path=/; HttpOnly; SameSite=Lax", NGAdaptorJava.setCookieHeaderValue( new NGCookie( "a", "1" ) ) );
	}
}
