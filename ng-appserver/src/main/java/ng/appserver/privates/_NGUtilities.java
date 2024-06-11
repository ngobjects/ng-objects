package ng.appserver.privates;

/**
 * Repository for stuff that haven't found an actual home.
 * When times are good, this class will be empty.
 * When times are bad, it will be brimming with functionality.
 */

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

		if( object instanceof Number number ) {
			// Note that Number.doubleValue() might return Double.NaN which is... Troublesome. Trying to decide if NaN is true or false is almost a philosophical question.
			// I'm still leaning towards keeping our definition of "only exactly zero is false", meaning NaN is true, making this code currently fine.
			return number.doubleValue() != 0;
		}

		return true;
	}
}