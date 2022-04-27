package ng.appserver.templating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class TestNGDynamicHTMLTag {

	@Test
	public void testNameWithQuotes() {
		try {
			NGDynamicHTMLTag tag1 = new NGDynamicHTMLTag( "<wo name = \"_NGString_4\"", null );
			assertEquals( "_NGString_4", tag1.name() );

			NGDynamicHTMLTag tag2 = new NGDynamicHTMLTag( "<wo name=\"bleble\"", null );
			assertEquals( "bleble", tag2.name() );
		}
		catch( NGHelperFunctionHTMLFormatException e ) {
			fail();
		}
	}

	@Test
	public void testNameWithoutQuotes() {
		try {
			NGDynamicHTMLTag tag1 = new NGDynamicHTMLTag( "<wo name=_NGString_4", null );
			assertEquals( "_NGString_4", tag1.name() );

			NGDynamicHTMLTag tag2 = new NGDynamicHTMLTag( "<wo name=bleble", null );
			assertEquals( "bleble", tag2.name() );
		}
		catch( NGHelperFunctionHTMLFormatException e ) {
			fail();
		}
	}
}