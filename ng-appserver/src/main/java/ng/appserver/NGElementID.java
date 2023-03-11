package ng.appserver;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an elementID.
 */

public class NGElementID {

	/**
	 * Character used to separate the individual components of the element ID
	 */
	private static final char COMPONENT_SEPARATOR = '.';

	/**
	 * The index of the component we're currently manipulating in components[].
	 * This is effectively the length of the elementID -1
	 */
	private int index = -1;

	/**
	 * The components of the elementID.
	 *
	 * FIXME: This array needs to be dynamically resized // Hugi 2023-02-09
	 */
	private final int[] components = new int[10];

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

	public static NGElementID fromString( final String elementIDString ) {
		final NGElementID id = new NGElementID();
		final String[] split = elementIDString.split( "\\." );

		for( int i = 0; i < split.length; i++ ) {
			id.components[i] = Integer.parseInt( split[i] );
		}

		id.index = split.length - 1;

		return id;
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
				b.append( COMPONENT_SEPARATOR );
			}
		}

		return b.toString();
	}

	@Override
	public boolean equals( Object obj ) {

		if( obj instanceof NGElementID ng ) {
			return Arrays.equals( components, ng.components ) && index == ng.index;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash( components, index );
	}
}