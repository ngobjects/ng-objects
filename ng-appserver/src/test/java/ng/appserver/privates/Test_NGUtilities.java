package ng.appserver.privates;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class Test_NGUtilities {

	@Test
	public void isTruthy() {
		assertFalse( _NGUtilities.isTruthy( null ) );

		assertFalse( _NGUtilities.isTruthy( false ) );
		assertTrue( _NGUtilities.isTruthy( true ) );

		assertFalse( _NGUtilities.isTruthy( Boolean.FALSE ) );
		assertTrue( _NGUtilities.isTruthy( Boolean.TRUE ) );

		assertFalse( _NGUtilities.isTruthy( 0 ) );
		assertTrue( _NGUtilities.isTruthy( 0.1 ) );
		assertTrue( _NGUtilities.isTruthy( 2 ) );

		assertFalse( _NGUtilities.isTruthy( BigDecimal.ZERO ) );
		assertTrue( _NGUtilities.isTruthy( new BigDecimal( "2.5" ) ) );

		assertTrue( _NGUtilities.isTruthy( "haha" ) );
		assertTrue( _NGUtilities.isTruthy( new Object() ) );
	}
}