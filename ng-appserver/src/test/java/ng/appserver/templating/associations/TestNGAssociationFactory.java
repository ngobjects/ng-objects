package ng.appserver.templating.associations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestNGAssociationFactory {

	@Test
	public void isNumeric() {
		assertFalse( NGAssociationFactory.isNumeric( "a" ) );
		assertFalse( NGAssociationFactory.isNumeric( "" ) );
		assertFalse( NGAssociationFactory.isNumeric( "." ) );
		assertFalse( NGAssociationFactory.isNumeric( "+" ) );
		assertFalse( NGAssociationFactory.isNumeric( "-" ) );
		assertFalse( NGAssociationFactory.isNumeric( "1+2" ) );
		assertFalse( NGAssociationFactory.isNumeric( "1-2" ) );
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