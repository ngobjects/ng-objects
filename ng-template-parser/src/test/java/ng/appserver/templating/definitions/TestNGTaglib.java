package ng.appserver.templating.definitions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Reading parsley.taglib: the names of the elements a library publishes, nothing more.
 */
public class TestNGTaglib {

	public static NGTaglib parse( final String xml ) {
		return NGTaglib.parse( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ), "test" );
	}

	@Test
	public void elementNames() {
		final NGTaglib taglib = parse( """
				<taglib>
					<element name="NGString"/>
					<element name="Badge"/>
				</taglib>
				""" );

		assertEquals( List.of( "NGString", "Badge" ), taglib.elementNames() );
	}

	@Test
	public void anElementNeedsAName() {
		final IllegalArgumentException e = assertThrows( IllegalArgumentException.class, () -> parse( "<taglib><element/></taglib>" ) );
		assertTrue( e.getMessage().contains( "name" ) );
	}

	@Test
	public void theRootMustBeTaglib() {
		assertThrows( IllegalArgumentException.class, () -> parse( "<library/>" ) );
	}

	@Test
	public void unknownChildrenAreIgnored() {
		// A newer file must still load in an older runtime
		assertEquals( List.of( "X" ), parse( "<taglib><version>2</version><element name=\"X\"/></taglib>" ).elementNames() );
	}

	@Test
	public void doctypesAreRefused() {
		assertThrows( IllegalArgumentException.class, () -> parse( "<!DOCTYPE taglib [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><taglib><element name=\"&x;\"/></taglib>" ) );
	}
}
