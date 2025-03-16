package ng.appserver.privates;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ng.appserver.templating.NGElementUtils;

public class Test_NGUtilities {

	@Test
	public void isTruthy() {
		assertFalse( NGElementUtils.isTruthy( null ) );

		assertFalse( NGElementUtils.isTruthy( false ) );
		assertTrue( NGElementUtils.isTruthy( true ) );

		assertFalse( NGElementUtils.isTruthy( Boolean.FALSE ) );
		assertTrue( NGElementUtils.isTruthy( Boolean.TRUE ) );

		assertFalse( NGElementUtils.isTruthy( 0 ) );
		assertTrue( NGElementUtils.isTruthy( 0.1 ) );
		assertTrue( NGElementUtils.isTruthy( 2 ) );
		assertTrue( NGElementUtils.isTruthy( Double.NaN ) );

		assertFalse( NGElementUtils.isTruthy( BigDecimal.ZERO ) );
		assertTrue( NGElementUtils.isTruthy( new BigDecimal( "2.5" ) ) );

		assertTrue( NGElementUtils.isTruthy( "haha" ) );
		assertTrue( NGElementUtils.isTruthy( new Object() ) );
	}
}