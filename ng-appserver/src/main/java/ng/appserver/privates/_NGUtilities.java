package ng.appserver.privates;

public class _NGUtilities {

	/**
	 * @return true if the given object is "truthy" for conditionals
	 *
	 * The only conditions for returning false are:
	 * - Boolean false (box or primitive)
	 * - A number that's exactly zero
	 * - null
	 */
	public static boolean isTruthy( Object object ) {

		if( object == null ) {
			return false;
		}

		if( object instanceof Boolean b ) {
			return b;
		}

		if( object instanceof Number n ) {
			// Note that doubleValue might return Double.NaN which is... Troublesome. Trying to decide if NaN is true or false is almost a philosophical question.
			// I'm still leaning towards keeping our definition of "only exactly zero is false", meaning NaN is true, making this code currently fine.
			return n.doubleValue() != 0;
		}

		return true;
	}
}