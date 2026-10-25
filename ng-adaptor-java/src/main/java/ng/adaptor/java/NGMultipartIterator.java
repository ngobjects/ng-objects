package ng.adaptor.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import ng.appserver.http.NGMultipartRequest.Part;

/**
 * A multipart/form-data body's parts (RFC 7578), read from the body as they're asked for. Its constructor is this adaptor's NGMultipartParser.
 *
 * A part's content is readable until the next part is asked for. Moving on skips whatever of it wasn't read.
 *
 * Parsing is lenient where clients differ and strict where the body would otherwise be misread:
 *
 * - The preamble before the first boundary and the epilogue after the last are ignored, as is whitespace after a boundary. Within a part's headers, and after a boundary, a line break may be a bare LF; the line break before a boundary must be CRLF.
 * - A part's name and filename come from its Content-Disposition header. A quoted value may escape characters with a backslash, and the percent-escapes browsers use for '"', CR and LF are decoded. filename* (RFC 5987) takes precedence over filename. A Windows path given as a filename (as old browsers sent) is cut to its last segment.
 * - A part without a name isn't a form field and is skipped.
 * - A body that ends before its closing boundary, or a part whose headers are longer than MAX_HEADERS_LENGTH, fails.
 */

public class NGMultipartIterator implements Iterator<Part> {

	/**
	 * The longest a part's headers may be, in bytes
	 */
	public static final int MAX_HEADERS_LENGTH = 16 * 1024;

	private static final byte CR = '\r';
	private static final byte LF = '\n';

	private final InputStream _body;

	/**
	 * What separates parts within the body: a line break, two dashes and the boundary
	 */
	private final byte[] _delimiter;

	/**
	 * Bytes read from the body and not yet consumed: _buffer[_start] to _buffer[_end]
	 */
	private byte[] _buffer = new byte[64 * 1024];
	private int _start;
	private int _end;

	/**
	 * true once the body has been read to its end
	 */
	private boolean _bodyEnded;

	/**
	 * true once the closing boundary has been read
	 */
	private boolean _complete;

	/**
	 * true once the body has been read up to the first boundary
	 */
	private boolean _started;

	/**
	 * A part whose headers have been read, waiting to be handed over. null if there's none.
	 */
	private Part _next;

	/**
	 * The content of the part handed over last. null before the first.
	 */
	private PartContent _handedOver;

	public NGMultipartIterator( final InputStream body, final String boundary ) {
		_body = body;
		_delimiter = ("\r\n--" + boundary).getBytes( StandardCharsets.ISO_8859_1 );

		// The first boundary needs no line break before it. Pretending there was one lets it be found like any other.
		_buffer[0] = CR;
		_buffer[1] = LF;
		_end = 2;
	}

	@Override
	public boolean hasNext() {
		if( _next != null ) {
			return true;
		}

		if( _complete ) {
			return false;
		}

		if( !_started ) {
			skipToDelimiter();
			_started = true;
		}
		else if( _handedOver != null ) {
			// Skip whatever's left of the part handed over last
			_handedOver.close();

			while( !_handedOver._ended ) {
				_handedOver.next( _buffer.length );
			}
		}

		if( !readAfterDelimiter() ) {
			_complete = true;
			return false;
		}

		_next = readPart();
		return _next != null;
	}

	@Override
	public Part next() {
		if( !hasNext() ) {
			throw new NoSuchElementException();
		}

		final Part part = _next;
		_next = null;
		_handedOver = (PartContent)part.stream();
		return part;
	}

	/**
	 * Reads past everything before the first delimiter (the preamble), and the delimiter itself
	 */
	private void skipToDelimiter() {
		while( true ) {
			final int index = indexOfDelimiter();

			if( index != -1 ) {
				_start = index + _delimiter.length;
				return;
			}

			// Keep what might be the start of a delimiter split across reads
			_start = Math.max( _start, _end - _delimiter.length + 1 );
			requireMore( "before its first boundary" );
		}
	}

	/**
	 * Reads what follows a delimiter
	 *
	 * @return true if a part follows, false if it was the closing delimiter
	 */
	private boolean readAfterDelimiter() {
		ensure( 2, "after a boundary" );

		if( _buffer[_start] == '-' && _buffer[_start + 1] == '-' ) {
			return false;
		}

		// Whitespace may follow a boundary, then the line break that ends it
		while( true ) {
			ensure( 1, "after a boundary" );
			final byte b = _buffer[_start];

			if( b == ' ' || b == '\t' ) {
				_start++;
			}
			else {
				break;
			}
		}

		skipLineBreak( "after a boundary" );
		return true;
	}

	/**
	 * @return The part whose headers start here, read up to its content. null if no part with a name follows.
	 */
	private Part readPart() {
		final Map<String, List<String>> headers = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
		int headersLength = 0;

		while( true ) {
			final String line = readLine();
			headersLength += line.length() + 2;

			if( headersLength > MAX_HEADERS_LENGTH ) {
				throw new IllegalStateException( "A part's headers are longer than %s bytes".formatted( MAX_HEADERS_LENGTH ) );
			}

			if( line.isEmpty() ) {
				break;
			}

			final int colon = line.indexOf( ':' );

			if( colon > 0 ) {
				headers.computeIfAbsent( line.substring( 0, colon ).strip(), _unused -> new ArrayList<>() ).add( line.substring( colon + 1 ).strip() );
			}
		}

		final List<String> dispositions = headers.get( "content-disposition" );
		final Map<String, String> disposition = parameters( dispositions == null || dispositions.isEmpty() ? "" : dispositions.get( 0 ) );
		final PartContent content = new PartContent();

		final String name = disposition.get( "name" );

		if( name == null ) {
			// Not a form field. Skip it and move on to the next part, if there is one.
			while( !content._ended ) {
				content.next( _buffer.length );
			}

			if( !readAfterDelimiter() ) {
				_complete = true;
				return null;
			}

			return readPart();
		}

		return new Part( name, filename( disposition ), headers, () -> content, null );
	}

	/**
	 * @return The filename given by the given Content-Disposition parameters, filename* (RFC 5987) taking precedence. null if neither is given.
	 */
	private static String filename( final Map<String, String> disposition ) {
		final String extended = disposition.get( "filename*" );

		if( extended != null ) {
			final String decoded = decodeExtendedValue( extended );

			if( decoded != null ) {
				return decoded;
			}
		}

		final String filename = disposition.get( "filename" );

		if( filename != null && filename.length() > 2 && Character.isLetter( filename.charAt( 0 ) ) && filename.charAt( 1 ) == ':' && filename.charAt( 2 ) == '\\' ) {
			return filename.substring( filename.lastIndexOf( '\\' ) + 1 );
		}

		return filename;
	}

	/**
	 * @return The given RFC 5987 value (charset'language'percent-encoded) decoded. null if it isn't one, or names an unknown charset.
	 */
	private static String decodeExtendedValue( final String value ) {
		final int firstQuote = value.indexOf( '\'' );
		final int secondQuote = firstQuote == -1 ? -1 : value.indexOf( '\'', firstQuote + 1 );

		if( secondQuote == -1 ) {
			return null;
		}

		try {
			final Charset charset = Charset.forName( value.substring( 0, firstQuote ) );
			return URLDecoder.decode( value.substring( secondQuote + 1 ).replace( "+", "%2B" ), charset );
		}
		catch( IllegalArgumentException e ) {
			return null;
		}
	}

	/**
	 * @return The parameters of a header value like Content-Disposition's (form-data; name="field"; filename="a.txt"), by lowercased name, values unquoted
	 */
	static Map<String, String> parameters( final String headerValue ) {
		final Map<String, String> parameters = new TreeMap<>();
		int i = headerValue.indexOf( ';' );

		while( i != -1 && i < headerValue.length() ) {
			i++;

			final int equals = headerValue.indexOf( '=', i );

			if( equals == -1 ) {
				break;
			}

			final String name = headerValue.substring( i, equals ).strip().toLowerCase();
			int j = equals + 1;

			while( j < headerValue.length() && headerValue.charAt( j ) == ' ' ) {
				j++;
			}

			final StringBuilder value = new StringBuilder();

			if( j < headerValue.length() && headerValue.charAt( j ) == '"' ) {
				j++;

				while( j < headerValue.length() && headerValue.charAt( j ) != '"' ) {
					final char c = headerValue.charAt( j );

					// A backslash escapes the next character, unless it ends the value (as in a Windows path's last separator)
					if( c == '\\' && j + 1 < headerValue.length() && (headerValue.charAt( j + 1 ) == '"' || headerValue.charAt( j + 1 ) == '\\') ) {
						value.append( headerValue.charAt( j + 1 ) );
						j += 2;
					}
					else {
						value.append( c );
						j++;
					}
				}

				parameters.put( name, value.toString().replace( "%22", "\"" ).replace( "%0D", "\r" ).replace( "%0A", "\n" ) );
				i = headerValue.indexOf( ';', j );
			}
			else {
				final int semicolon = headerValue.indexOf( ';', j );
				parameters.put( name, headerValue.substring( j, semicolon == -1 ? headerValue.length() : semicolon ).strip() );
				i = semicolon;
			}
		}

		return parameters;
	}

	/**
	 * @return The next line of headers, without its line break, decoded as UTF-8 (which browsers send names and filenames in)
	 */
	private String readLine() {
		while( true ) {
			for( int k = _start; k < _end; k++ ) {
				if( _buffer[k] == LF ) {
					final int lineEnd = k > _start && _buffer[k - 1] == CR ? k - 1 : k;
					final String line = new String( _buffer, _start, lineEnd - _start, StandardCharsets.UTF_8 );
					_start = k + 1;
					return line;
				}
			}

			if( _end - _start > MAX_HEADERS_LENGTH ) {
				throw new IllegalStateException( "A part's headers are longer than %s bytes".formatted( MAX_HEADERS_LENGTH ) );
			}

			requireMore( "within a part's headers" );
		}
	}

	private void skipLineBreak( final String where ) {
		ensure( 1, where );

		if( _buffer[_start] == CR ) {
			_start++;
			ensure( 1, where );
		}

		if( _buffer[_start] != LF ) {
			throw new IllegalStateException( "The multipart body has something other than a line break %s".formatted( where ) );
		}

		_start++;
	}

	/**
	 * @return The index of the next delimiter in the buffer, -1 if there's none (yet)
	 */
	private int indexOfDelimiter() {
		outer: for( int k = _start; k <= _end - _delimiter.length; k++ ) {
			for( int d = 0; d < _delimiter.length; d++ ) {
				if( _buffer[k + d] != _delimiter[d] ) {
					continue outer;
				}
			}

			return k;
		}

		return -1;
	}

	/**
	 * Makes sure at least the given number of bytes is in the buffer
	 */
	private void ensure( final int count, final String where ) {
		while( _end - _start < count ) {
			requireMore( where );
		}
	}

	/**
	 * Reads more of the body into the buffer
	 *
	 * @throws IllegalStateException If the body has ended
	 */
	private void requireMore( final String where ) {
		if( _bodyEnded ) {
			throw new IllegalStateException( "The multipart body ended %s".formatted( where ) );
		}

		// Make room: move what's unconsumed to the start, growing the buffer if it's full
		if( _start > 0 ) {
			System.arraycopy( _buffer, _start, _buffer, 0, _end - _start );
			_end -= _start;
			_start = 0;
		}

		if( _end == _buffer.length ) {
			_buffer = Arrays.copyOf( _buffer, _buffer.length * 2 );
		}

		try {
			final int read = _body.read( _buffer, _end, _buffer.length - _end );

			if( read == -1 ) {
				_bodyEnded = true;
			}
			else {
				_end += read;
			}
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * A part's content, read from the body up to the next delimiter as it's asked for
	 */
	private class PartContent extends InputStream {

		/**
		 * true once the delimiter ending this part has been reached (and consumed)
		 */
		private boolean _ended;

		/**
		 * true once the next part has been asked for
		 */
		private boolean _closed;

		/**
		 * Content of this part available in the buffer, starting at _start. Determined by next().
		 */
		private int _available;

		/**
		 * Makes content available, up to the given number of bytes: everything up to the delimiter if it's in the buffer, otherwise everything that can't be the start of one
		 */
		void next( final int max ) {
			if( _available > 0 ) {
				_start += Math.min( _available, max );
				_available = 0;
				return;
			}

			while( true ) {
				final int index = indexOfDelimiter();

				if( index != -1 ) {
					if( index == _start ) {
						_start += _delimiter.length;
						_ended = true;
						return;
					}

					_start += Math.min( index - _start, max );
					return;
				}

				final int safe = _end - _delimiter.length + 1;

				if( safe > _start ) {
					_start += Math.min( safe - _start, max );
					return;
				}

				requireMore( "before its closing boundary" );
			}
		}

		/**
		 * @return The number of bytes of this part's content available at _start, 0 at its end
		 */
		private int available( final int max ) throws IOException {
			if( _closed ) {
				throw new IOException( "This part's content is no longer readable, since the next part has been asked for" );
			}

			while( _available == 0 && !_ended ) {
				final int index = indexOfDelimiter();

				if( index == _start ) {
					_start += _delimiter.length;
					_ended = true;
				}
				else if( index != -1 ) {
					_available = index - _start;
				}
				else if( _end - _delimiter.length + 1 > _start ) {
					_available = _end - _delimiter.length + 1 - _start;
				}
				else {
					requireMore( "before its closing boundary" );
				}
			}

			return Math.min( _available, max );
		}

		@Override
		public int read() throws IOException {
			if( available( 1 ) == 0 ) {
				return -1;
			}

			final int b = _buffer[_start] & 0xFF;
			_start++;
			_available--;
			return b;
		}

		@Override
		public int read( final byte[] b, final int off, final int len ) throws IOException {
			if( len == 0 ) {
				return 0;
			}

			final int count = available( len );

			if( count == 0 ) {
				return -1;
			}

			System.arraycopy( _buffer, _start, b, off, count );
			_start += count;
			_available -= count;
			return count;
		}

		@Override
		public void close() {
			_closed = true;
		}
	}
}
