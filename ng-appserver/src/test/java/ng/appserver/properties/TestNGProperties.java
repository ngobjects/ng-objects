package ng.appserver.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.properties.NGProperties.PropertiesSourceArguments;

public class TestNGProperties {

	@Test
	public void readPropertySourceArguments() {

		final String[] argv = new String[] {
				"-Dpass=SomePass",

				"-name",
				"hugi",

				"-Dhi=there",

				"-WOPort",
				"1300",

				"-Xmx2G",
				"Bla"
		};

		final Map<String, String> props = new PropertiesSourceArguments( argv ).readAll();

		assertEquals( "SomePass", props.get( "pass" ) );
		assertEquals( "SomePass", System.getProperty( "pass" ) );

		assertEquals( "hugi", props.get( "name" ) );
		assertNull( System.getProperty( "name" ) );

		assertEquals( "there", props.get( "hi" ) );
		assertEquals( "there", System.getProperty( "hi" ) );

		assertEquals( "1300", props.get( "WOPort" ) );
		assertEquals( "1300", props.get( "WOPort" ) );
	}
}