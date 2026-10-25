package ng.appserver.wointegration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGRequest;
import ng.appserver.http.NGRequests;

public class TestNGWOAccessGuard {

	@Test
	public void permitsLoopback() {
		assertTrue( NGWOAccessGuard.permits( request( "127.0.0.1", Map.of() ) ) );
		assertTrue( NGWOAccessGuard.permits( request( "0:0:0:0:0:0:0:1", Map.of() ) ) );
		assertTrue( NGWOAccessGuard.permits( request( "[::1]", Map.of() ) ) );
	}

	@Test
	public void permitsAnAddressOfAnotherOfThisHostsInterfaces() throws SocketException {
		final InetAddress address = Collections.list( NetworkInterface.getNetworkInterfaces() )
				.stream()
				.filter( networkInterface -> !isLoopback( networkInterface ) )
				.flatMap( NetworkInterface::inetAddresses )
				.findFirst()
				.orElse( null );

		assumeTrue( address != null, "This host has no network interface besides loopback" );
		assertTrue( NGWOAccessGuard.permits( request( address.getHostAddress(), Map.of() ) ) );
	}

	@Test
	public void refusesARemoteAddress() {
		assertFalse( NGWOAccessGuard.permits( request( "203.0.113.7", Map.of() ) ) );
	}

	@Test
	public void refusesAnUnknownAddress() {
		assertFalse( NGWOAccessGuard.permits( request( null, Map.of() ) ) );
		assertFalse( NGWOAccessGuard.permits( request( "", Map.of() ) ) );
	}

	@Test
	public void refusesAHostNameRatherThanResolvingIt() {
		assertFalse( NGWOAccessGuard.permits( request( "localhost", Map.of() ) ) );
	}

	@Test
	public void refusesALocalRequestThroughTheWebServerAdaptor() {
		assertFalse( NGWOAccessGuard.permits( request( "127.0.0.1", Map.of( "X-WebObjects-Adaptor-Version", List.of( "Apache" ) ) ) ) );
	}

	@Test
	public void refusesALocalRequestThroughAProxy() {
		assertFalse( NGWOAccessGuard.permits( request( "127.0.0.1", Map.of( "X-Forwarded-For", List.of( "203.0.113.7" ) ) ) ) );
		assertFalse( NGWOAccessGuard.permits( request( "127.0.0.1", Map.of( "Forwarded", List.of( "for=203.0.113.7" ) ) ) ) );
	}

	private static NGRequest request( final String remoteAddress, final Map<String, List<String>> headers ) {
		return NGRequests.of( "POST", "/womp/instanceRequest", null, headers, InputStream.nullInputStream(), remoteAddress, null );
	}

	private static boolean isLoopback( final NetworkInterface networkInterface ) {
		try {
			return networkInterface.isLoopback();
		}
		catch( final SocketException e ) {
			throw new RuntimeException( e );
		}
	}
}
