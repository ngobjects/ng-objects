package ng.appserver.templating.parser;

import java.util.NoSuchElementException;

public class NGStringTokenizer {

	public int currentPosition;
	private int newPosition;
	private int maxPosition;
	private String str;
	private String delimiters;
	private boolean retDelims;
	private boolean delimsChanged;
	private int maxDelimCodePoint;
	private boolean hasSurrogates = false;
	private int[] delimiterCodePoints;

	private void setMaxDelimCodePoint() {
		if( delimiters == null ) {
			maxDelimCodePoint = 0;
			return;
		}

		int m = 0;
		int c;
		int count = 0;
		for( int i = 0; i < delimiters.length(); i += Character.charCount( c ) ) {
			c = delimiters.charAt( i );
			if( c >= Character.MIN_HIGH_SURROGATE && c <= Character.MAX_LOW_SURROGATE ) {
				c = delimiters.codePointAt( i );
				hasSurrogates = true;
			}
			if( m < c ) {
				m = c;
			}
			count++;
		}
		maxDelimCodePoint = m;

		if( hasSurrogates ) {
			delimiterCodePoints = new int[count];
			for( int i = 0, j = 0; i < count; i++, j += Character.charCount( c ) ) {
				c = delimiters.codePointAt( j );
				delimiterCodePoints[i] = c;
			}
		}
	}

	public NGStringTokenizer( String str, String delim ) {
		currentPosition = 0;
		newPosition = -1;
		delimsChanged = false;
		this.str = str;
		maxPosition = str.length();
		delimiters = delim;
		retDelims = false;
		setMaxDelimCodePoint();
	}

	private int skipDelimiters( int startPos ) {
		if( delimiters == null ) {
			throw new NullPointerException();
		}

		int position = startPos;
		while( !retDelims && position < maxPosition ) {
			if( !hasSurrogates ) {
				char c = str.charAt( position );
				if( (c > maxDelimCodePoint) || (delimiters.indexOf( c ) < 0) ) {
					break;
				}
				position++;
			}
			else {
				int c = str.codePointAt( position );
				if( (c > maxDelimCodePoint) || !isDelimiter( c ) ) {
					break;
				}
				position += Character.charCount( c );
			}
		}
		return position;
	}

	private int scanToken( int startPos ) {
		int position = startPos;
		while( position < maxPosition ) {
			if( !hasSurrogates ) {
				char c = str.charAt( position );
				if( (c <= maxDelimCodePoint) && (delimiters.indexOf( c ) >= 0) ) {
					break;
				}
				position++;
			}
			else {
				int c = str.codePointAt( position );
				if( (c <= maxDelimCodePoint) && isDelimiter( c ) ) {
					break;
				}
				position += Character.charCount( c );
			}
		}
		if( retDelims && (startPos == position) ) {
			if( !hasSurrogates ) {
				char c = str.charAt( position );
				if( (c <= maxDelimCodePoint) && (delimiters.indexOf( c ) >= 0) ) {
					position++;
				}
			}
			else {
				int c = str.codePointAt( position );
				if( (c <= maxDelimCodePoint) && isDelimiter( c ) ) {
					position += Character.charCount( c );
				}
			}
		}
		return position;
	}

	private boolean isDelimiter( int codePoint ) {
		for( int delimiterCodePoint : delimiterCodePoints ) {
			if( delimiterCodePoint == codePoint ) {
				return true;
			}
		}
		return false;
	}

	public boolean hasMoreTokens() {
		/*
		 * Temporarily store this position and use it in the following
		 * nextToken() method only if the delimiters haven't been changed in
		 * that nextToken() invocation.
		 */
		newPosition = skipDelimiters( currentPosition );
		return (newPosition < maxPosition);
	}

	public String nextToken() {
		/*
		 * If next position already computed in hasMoreElements() and
		 * delimiters have changed between the computation and this invocation,
		 * then use the computed value.
		 */

		currentPosition = (newPosition >= 0 && !delimsChanged) ? newPosition : skipDelimiters( currentPosition );

		/* Reset these anyway */
		delimsChanged = false;
		newPosition = -1;

		if( currentPosition >= maxPosition ) {
			throw new NoSuchElementException();
		}
		int start = currentPosition;
		currentPosition = scanToken( currentPosition );
		return str.substring( start, currentPosition );
	}

	public String nextToken( String delim ) {
		delimiters = delim;

		/* delimiter string specified, so set the appropriate flag. */
		delimsChanged = true;

		setMaxDelimCodePoint();
		return nextToken();
	}
}