package ng.dev;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestNGDevLoopback {

	@Test
	public void ipv4Loopback() {
		assertTrue( NGDevLoopback.isLoopback( "127.0.0.1" ) );
		assertTrue( NGDevLoopback.isLoopback( "127.1.2.3" ) );
	}

	@Test
	public void ipv6Loopback() {
		// The macOS-resolves-localhost-to-::1 case the literal check missed.
		assertTrue( NGDevLoopback.isLoopback( "::1" ) );
		assertTrue( NGDevLoopback.isLoopback( "0:0:0:0:0:0:0:1" ) );
	}

	@Test
	public void decoratedForms() {
		assertTrue( NGDevLoopback.isLoopback( "/127.0.0.1" ) );        // InetSocketAddress.toString() prefix
		assertTrue( NGDevLoopback.isLoopback( "127.0.0.1:52344" ) );   // with a port
		assertTrue( NGDevLoopback.isLoopback( "[::1]" ) );             // bracketed IPv6
		assertTrue( NGDevLoopback.isLoopback( "[::1]:52344" ) );       // bracketed IPv6 with port
		assertTrue( NGDevLoopback.isLoopback( "localhost" ) );
	}

	@Test
	public void nonLoopbackRejected() {
		assertFalse( NGDevLoopback.isLoopback( "10.0.0.5" ) );
		assertFalse( NGDevLoopback.isLoopback( "192.168.1.20" ) );
		assertFalse( NGDevLoopback.isLoopback( "8.8.8.8" ) );
	}

	@Test
	public void unknownAddressFailsOpen() {
		// Dev-mode-only callers accept "unknown" as local; an adaptor that doesn't report the
		// address shouldn't lock out the local developer.
		assertTrue( NGDevLoopback.isLoopback( null ) );
		assertTrue( NGDevLoopback.isLoopback( "" ) );
		assertTrue( NGDevLoopback.isLoopback( "   " ) );
	}
}
