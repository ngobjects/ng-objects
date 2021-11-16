package ng.appserver;

/**
 * Wraps a cookie
 */

// FIXME: We need to decide how to handle timeouts.
// FIXME: We need to decide how to set-cookie. And even if that should be a separate class, since requests will only contain key-value pairs

public record NGCookie(
		String name,
		String value,
		String domain,
		String path,
		boolean isSecure,
		int maxAge ) {
}