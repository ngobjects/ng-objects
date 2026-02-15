package ng.appserver.templating.parser;

public class NGHTMLFormatException extends Exception {

	/**
	 * The character offset in the source where the error occurred, or -1 if unknown
	 */
	private final int _position;

	/**
	 * The source string being parsed when the error occurred, or null if unavailable
	 */
	private final String _source;

	/**
	 * Constructs an exception with a message, position, and the source being parsed.
	 * The message will be augmented with line/column info.
	 */
	public NGHTMLFormatException( String message, int position, String source ) {
		super( buildMessage( message, position, source ) );
		_position = position;
		_source = source;
	}

	/**
	 * Constructs an exception with just a message (no position info).
	 * Used by the legacy parser and for errors where position is not meaningful.
	 */
	public NGHTMLFormatException( String message ) {
		super( message );
		_position = -1;
		_source = null;
	}

	/**
	 * @return The source string being parsed when the error occurred, or null if unavailable
	 */
	public String source() {
		return _source;
	}

	/**
	 * @return The character offset in the source where the error occurred, or -1 if unknown
	 */
	public int position() {
		return _position;
	}

	/**
	 * @return The 1-based line number where the error occurred, or -1 if position is unknown
	 */
	public int line() {
		if( _position < 0 || _source == null ) {
			return -1;
		}

		return lineForPosition( _source, _position );
	}

	/**
	 * @return The 1-based column number where the error occurred, or -1 if position is unknown
	 */
	public int column() {
		if( _position < 0 || _source == null ) {
			return -1;
		}

		return columnForPosition( _source, _position );
	}

	/**
	 * Builds an error message that includes the original message and line/column info.
	 */
	private static String buildMessage( final String message, final int position, final String source ) {

		if( source == null || position < 0 ) {
			return message;
		}

		final int line = lineForPosition( source, position );
		final int column = columnForPosition( source, position );

		return "%s (line %d, column %d)".formatted( message, line, column );
	}

	/**
	 * @return The 1-based line number for the given position in the source
	 */
	private static int lineForPosition( final String source, final int position ) {
		int line = 1;
		final int clampedPos = Math.min( position, source.length() );

		for( int i = 0; i < clampedPos; i++ ) {
			if( source.charAt( i ) == '\n' ) {
				line++;
			}
		}

		return line;
	}

	/**
	 * @return The 1-based column number for the given position in the source
	 */
	private static int columnForPosition( final String source, final int position ) {
		final int clampedPos = Math.min( position, source.length() );
		int lastNewline = -1;

		for( int i = 0; i < clampedPos; i++ ) {
			if( source.charAt( i ) == '\n' ) {
				lastNewline = i;
			}
		}

		return clampedPos - lastNewline;
	}

}
