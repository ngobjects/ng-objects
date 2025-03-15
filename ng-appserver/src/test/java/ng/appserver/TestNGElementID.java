package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ng.appserver.templating.NGElementID;

public class TestNGElementID {

	@Test
	public void testElementID() {
		NGElementID elementID = new NGElementID();
		assertEquals( "", elementID.toString() );

		elementID.addBranch();
		assertEquals( "0", elementID.toString() );

		elementID.addBranch();
		assertEquals( "0.0", elementID.toString() );

		elementID.increment();
		assertEquals( "0.1", elementID.toString() );

		elementID.addBranch();
		assertEquals( "0.1.0", elementID.toString() );

		elementID.increment();
		elementID.increment();
		assertEquals( "0.1.2", elementID.toString() );

		elementID.removeBranch();
		assertEquals( "0.1", elementID.toString() );
	}

	@Test
	public void testFromString() {
		assertEquals( NGElementID.fromString( "0.3.4" ).toString(), "0.3.4" );
		//		assertEquals( NGElementID.fromString( "" ), "" );
	}
}