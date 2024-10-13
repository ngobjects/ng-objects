package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestNGAssociationFactory {

	@Test
	public void isNumeric() {
		assertFalse( NGAssociationFactory.isNumeric( "" ) );
		assertTrue( NGAssociationFactory.isNumeric( "-1.345" ) );
		assertTrue( NGAssociationFactory.isNumeric( "+1456" ) );
		assertTrue( NGAssociationFactory.isNumeric( "+1.456987" ) );
		assertTrue( NGAssociationFactory.isNumeric( "-1456987" ) );
		assertTrue( NGAssociationFactory.isNumeric( "1456987" ) );
		assertTrue( NGAssociationFactory.isNumeric( "14.56987" ) );
		assertTrue( NGAssociationFactory.isNumeric( "-.876" ) );
		assertTrue( NGAssociationFactory.isNumeric( "-0.876" ) );
		assertTrue( NGAssociationFactory.isNumeric( ".876" ) );
	}
}