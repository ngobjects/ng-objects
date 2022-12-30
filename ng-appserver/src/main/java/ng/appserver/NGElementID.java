package ng.appserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an elementID.
 */

public class NGElementID {

	private final List<Integer> elements = new ArrayList<>();

	public void addBranch() {
		elements.add( 0 );
	}

	public void removeBranch() {
		elements.remove( elements.size() - 1 );
	}

	public void increment() {
		int lastValue = elements.remove( elements.size() - 1 );
		elements.add( lastValue + 1 );
	}

	@Override
	public String toString() {
		return String.join( ".", elements.stream().map( String::valueOf ).toList() );
	}

	@Override
	public boolean equals( Object obj ) {

		if( obj instanceof NGElementID ng ) {
			return elements.equals( ng.elements );
		}

		return false;
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}
}