package ng.appserver.privates;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ng.appserver.templating.assications.NGAssociationUtils;

public class Test_NGUtilities {

	@Test
	public void isTruthy() {
		assertFalse( NGAssociationUtils.isTruthy( null ) );

		assertFalse( NGAssociationUtils.isTruthy( false ) );
		assertTrue( NGAssociationUtils.isTruthy( true ) );

		assertFalse( NGAssociationUtils.isTruthy( Boolean.FALSE ) );
		assertTrue( NGAssociationUtils.isTruthy( Boolean.TRUE ) );

		assertFalse( NGAssociationUtils.isTruthy( 0 ) );
		assertTrue( NGAssociationUtils.isTruthy( 0.1 ) );
		assertTrue( NGAssociationUtils.isTruthy( 2 ) );
		assertTrue( NGAssociationUtils.isTruthy( Double.NaN ) );

		assertFalse( NGAssociationUtils.isTruthy( BigDecimal.ZERO ) );
		assertTrue( NGAssociationUtils.isTruthy( new BigDecimal( "2.5" ) ) );

		assertTrue( NGAssociationUtils.isTruthy( "haha" ) );
		assertTrue( NGAssociationUtils.isTruthy( new Object() ) );
	}
}