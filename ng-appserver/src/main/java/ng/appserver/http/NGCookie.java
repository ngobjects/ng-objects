package ng.appserver.http;

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
	}

	public NGCookie( final String name, final String value, final Long maxAge ) {
		this( name, value, null, "/", maxAge, false, true, "Lax" );
	}

	public NGCookie( final String name, final String value ) {
		this( name, value, null );
	}
}