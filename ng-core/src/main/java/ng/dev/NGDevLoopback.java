package ng.dev;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Decides whether a request's client address is the loopback interface — the gate the dangerous
 * dev endpoints (notably /eval) use to restrict themselves to local callers. Shared by both
 * frameworks so the rule is defined once and can't drift between them.
 *
 * <p>Uses {@link InetAddress#isLoopbackAddress()} rather than matching literal strings, because the
 * address an adaptor hands us takes many forms — {@code 127.0.0.1}, {@code ::1},
 * {@code 0:0:0:0:0:0:0:1}, any {@code 127.x.y.z}, sometimes with a leading {@code /} (from
 * {@code InetSocketAddress.toString()}) or wrapped in {@code [...]}. macOS resolving
 * {@code localhost} to {@code ::1} is the common case that literal-matching missed.
 */
public class NGDevLoopback {

	private NGDevLoopback() {}

	/**
	 * @param remoteAddress the client's network address as the adaptor reported it, or null/blank if unknown
	 * @return true when the address is a loopback address, or is unknown ( failing open is acceptable
	 *         only because the callers are already dev-mode-only )
	 */
	public static boolean isLoopback( final String remoteAddress ) {

		if( remoteAddress == null || remoteAddress.isBlank() ) {
			return true;
		}

		final String cleaned = clean( remoteAddress );

		// The literal hostname never reaches InetAddress without a lookup; treat it directly.
		if( cleaned.equalsIgnoreCase( "localhost" ) ) {
			return true;
		}

		try {
			// getByName on a numeric address just parses it — no DNS — so this doesn't do network I/O
			// for the addresses adaptors actually report (they're numeric).
			return InetAddress.getByName( cleaned ).isLoopbackAddress();
		}
		catch( final UnknownHostException e ) {
			return false;
		}
	}

	/**
	 * Strips the decorations an address string can carry: a leading {@code /}, IPv6 {@code [...]}
	 * brackets, and a trailing {@code :port} (only when it's unambiguous — a bare IPv6 address has
	 * many colons, so we only strip a port from a bracketed address or a single-colon IPv4 form).
	 */
	private static String clean( final String raw ) {

		String s = raw.strip();

		if( s.startsWith( "/" ) ) {
			s = s.substring( 1 );
		}

		if( s.startsWith( "[" ) ) {
			final int close = s.indexOf( ']' );
			if( close >= 0 ) {
				return s.substring( 1, close ); // inside the brackets; drop any :port after ]
			}
		}

		// IPv4 with a port ( exactly one colon ) — drop the port. A bare IPv6 address has 2+ colons
		// and no brackets, so leave it alone.
		final int firstColon = s.indexOf( ':' );
		if( firstColon >= 0 && s.indexOf( ':', firstColon + 1 ) < 0 ) {
			s = s.substring( 0, firstColon );
		}

		return s;
	}
}
