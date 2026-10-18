package ng.appserver.wointegration;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.http.NGRequest;

/**
 * TEMPORARY WORKAROUND for a defect in WO's deployment stack. Delete once wotaskd authenticates its instance requests.
 *
 * The defect: wotaskd sends instance requests (TERMINATE, REFUSE, STATISTICS…) with no credentials at all, so an application can't
 * tell wotaskd from anyone else who can reach its port. WO's own answer (WOAdminAction.instanceRequestAction()) is to judge the request
 * by where it came from, and this class does the same, so that ng is no weaker than WO under the wotaskd deployed today. Judging by
 * origin is not authentication. A reverse proxy on the same host that adds no forwarding headers gets straight through.
 *
 * A request is permitted only when
 *
 * - the client address is bound to one of this host's network interfaces (loopback included), and
 * - it didn't come through the WO web server adaptor (an 'x-webobjects-adaptor-version' header), and
 * - it didn't come through any other proxy (a 'forwarded' or 'x-forwarded-for' header).
 *
 * An unknown or unreadable client address is refused.
 *
 * The proper fix is a shared secret that wotaskd sends and the application checks (undur/wonder-slim-deployment#34 and #35). We can
 * make that change in our own fork of the deployment tools, but environments that haven't adopted them still run a wotaskd that sends
 * no credentials, so until they have, this stays.
 */

final class NGWOAccessGuard {

	private static final Logger logger = LoggerFactory.getLogger( NGWOAccessGuard.class );

	private NGWOAccessGuard() {}

	/**
	 * @return true if the request may be handled as an instance request from wotaskd
	 */
	static boolean permits( final NGRequest request ) {

		if( hasHeader( request, "x-webobjects-adaptor-version" ) ) {
			logger.warn( "Refused instance request from {}: it came through the web server adaptor", request.remoteAddress() );
			return false;
		}

		if( hasHeader( request, "forwarded" ) || hasHeader( request, "x-forwarded-for" ) ) {
			logger.warn( "Refused instance request from {}: it came through a proxy", request.remoteAddress() );
			return false;
		}

		final InetAddress address = parseAddress( request.remoteAddress() );

		if( address == null ) {
			logger.warn( "Refused instance request: unknown client address '{}'", request.remoteAddress() );
			return false;
		}

		if( !isLocal( address ) ) {
			logger.warn( "Refused instance request from {}: not one of this host's addresses", address.getHostAddress() );
			return false;
		}

		return true;
	}

	/**
	 * @return true if the address is bound to one of this host's network interfaces
	 */
	private static boolean isLocal( final InetAddress address ) {
		try {
			return NetworkInterface.getByInetAddress( address ) != null;
		}
		catch( final SocketException e ) {
			logger.error( "Couldn't look up the network interface for {}", address.getHostAddress(), e );
			return false;
		}
	}

	/**
	 * @return The client address the adaptor reported, null if it's missing or not an IP address
	 */
	private static InetAddress parseAddress( final String remoteAddress ) {

		if( remoteAddress == null || remoteAddress.isBlank() ) {
			return null;
		}

		String s = remoteAddress.strip();

		if( s.startsWith( "[" ) && s.endsWith( "]" ) ) {
			s = s.substring( 1, s.length() - 1 );
		}

		// Only IP literals (an IPv6 one possibly with a scope, as in 'fe80::1%en0'): anything else would make getByName() go to DNS
		if( !s.matches( "[0-9a-fA-F.:]+(%[0-9A-Za-z_.-]+)?" ) ) {
			return null;
		}

		try {
			return InetAddress.getByName( s );
		}
		catch( final UnknownHostException e ) {
			return null;
		}
	}

	private static boolean hasHeader( final NGRequest request, final String name ) {
		return !request.headersForKey( name ).isEmpty();
	}
}
