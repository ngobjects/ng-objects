package ng.appserver.http;

import java.time.Duration;
import java.util.Objects;

/**
 * A cookie. Yum!
 */

public record NGCookie( String name, String value, String domain, String path, Long maxAge, boolean secure, boolean httpOnly, String sameSite ) {

	public NGCookie {
		Objects.requireNonNull( name, "A cookie's [name] must not be null" );
		Objects.requireNonNull( value, "A cookie's [value] must not be null" );

		// Browsers reject SameSite=None cookies that aren't Secure, so failing at construction beats failing silently in the user's cookie jar
		if( "None".equals( sameSite ) && !secure ) {
			throw new IllegalArgumentException( "A cookie with SameSite=None must also be secure (browsers reject the combination otherwise)" );
		}

		// On the wire, a negative Max-Age would mean "expire immediately" (RFC 6265 treats anything <= 0 that way) —
		// but the cookie APIs our adaptors speak through use negative values as their sentinel for "no Max-Age attribute",
		// which would silently turn the cookie into a session cookie instead. Since the two meanings are opposites,
		// we reject the ambiguous value: use zero to delete a cookie, null for a session cookie.
		if( maxAge != null && maxAge < 0 ) {
			throw new IllegalArgumentException( "A cookie's [maxAge] must not be negative (use zero to delete a cookie, null for a session cookie)" );
		}
	}

	/**
	 * @param maxAge The cookie's lifetime. Zero deletes the cookie, null makes it a session cookie (lives until the browser closes). Sub-second precision is truncated.
	 */
	public NGCookie( final String name, final String value, final Duration maxAge ) {
		this( name, value, null, "/", maxAge == null ? null : maxAge.toSeconds(), false, true, "Lax" );
	}

	public NGCookie( final String name, final String value ) {
		this( name, value, null );
	}
}