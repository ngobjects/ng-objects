package ng.appserver.wointegration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.properties.NGProperties;
import ng.appserver.properties.NGProperties.PropertiesSource;

public class TestNGWOIntegrationPlugin {

	@Test
	public void hostNameIsWOHostWhenSet() {
		assertEquals( "example.com", NGWOIntegrationPlugin.hostName( properties( Map.of( "WOHost", "example.com" ) ) ) );
	}

	@Test
	public void hostNameDefaultsToThisMachinesName() throws UnknownHostException {
		assertEquals( InetAddress.getLocalHost().getHostName(), NGWOIntegrationPlugin.hostName( properties( Map.of() ) ) );
	}

	private static NGProperties properties( final Map<String, String> values ) {
		final NGProperties properties = new NGProperties();
		properties.addAndReadSource( new PropertiesSource() {

			@Override
			public Map<String, String> readAll() {
				return values;
			}

			@Override
			public String description() {
				return "test";
			}
		} );
		return properties;
	}
}
