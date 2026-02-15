package ng.appserver.templating.parser.model;

/**
 * Represents a range in the source template string.
 *
 * @param start The start offset (inclusive) in the source string
 * @param end The end offset (exclusive) in the source string
 */

public record SourceRange( int start, int end ) {

	public SourceRange {
		if( start < 0 ) {
			throw new IllegalArgumentException( "start must be non-negative, was: " + start );
		}
		if( end < start ) {
			throw new IllegalArgumentException( "end (%d) must be >= start (%d)".formatted( end, start ) );
		}
	}

	/**
	 * @return The length of this range
	 */
	public int length() {
		return end - start;
	}
}
