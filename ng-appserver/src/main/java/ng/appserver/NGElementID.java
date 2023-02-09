package ng.appserver;

import java.util.Arrays;

/**
 * Represents an elementID.
 */

public class NGElementID {

	/**
	 * The index of the component we're currently manipulating in components[]
	 */
	private int index = -1;

	/**
	 * The components of the elementID.
	 *
	 * Note that this was initially an int[] but was switched to a byte[] for performance, since these get used a lot on large pages.
	 * Using byte limits us to elementIDs no higher than 127. Although that's _unlikely_ to happen, it *could*, so we might have to reverse this decision later.
	 *
	 * FIXME: This array needs to be dynamically resized // Hugi 203-02-09
	 */
	private final byte[] components = new byte[10];

	public void addBranch() {
		index++;
	}

	public void removeBranch() {
		components[index] = 0;
		index--;
	}

	public void increment() {
		components[index]++;
	}

	@Override
	public String toString() {

		// An empty elementID is just an empty string.
		// ...Sigh. This is really just here satisfy the unit test, since this should never happen during actual execution.
		if( index == -1 ) {
			return "";
		}

		final int stringLengthGuess = index * 3; // We're venturing a guess that the max length of the elementID is three letter pr. component (two digits+period)
		final StringBuilder b = new StringBuilder( stringLengthGuess );

		for( int i = 0; i <= index; i++ ) {
			b.append( components[i] );

			if( i < index ) {
				b.append( '.' );
			}
		}

		return b.toString();
	}

	@Override
	public boolean equals( Object obj ) {

		if( obj instanceof NGElementID ng ) {
			Arrays.equals( components, ng.components );
		}

		return false;
	}

	@Override
	public int hashCode() {
		return components.hashCode();
	}
}