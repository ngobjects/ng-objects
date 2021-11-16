package ng.appserver;

/**
 * Wraps a cookie
 */

//FIXME: We need to decide how to handle timeouts.

public record NGCookie(
		String name,
		String value,
		String domain,
		String path,
		boolean isSecure,
		int maxAge ) {
}