package ng.adaptor.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.io.Content;

import ng.appserver.http.NGMessage;
import ng.appserver.http.NGMultipartRequest.Part;

/**
 * A multipart body's parts, read from the body as they're asked for. Jetty's parser does the parsing; this hands its parts over one at a time.
 *
 * A part's content is readable until the next part is asked for. Moving on skips whatever of it wasn't read.
 */

class NGIncomingParts implements Iterator<Part> {

	private final InputStream _body;
	private final MultiPart.Parser _parser;

	/**
	 * true once the whole body has been passed to the parser
	 */
	private boolean _bodyEnded;

	/**
	 * true once the parser has seen the body's closing boundary
	 */
	private boolean _complete;

	/**
	 * Why the parser failed. null if it hasn't.
	 */
	private Throwable _failure;

	/**
	 * Parts whose headers have been parsed, waiting to be handed over. One read of the body can hold several small parts.
	 */
	private final Deque<Part> _pending = new ArrayDeque<>();

	/**
	 * The content of the part being parsed
	 */
	private PartContent _parsing;

	/**
	 * The content of the part handed over last. null before the first.
	 */
	private PartContent _handedOver;

	NGIncomingParts( final InputStream body, final String boundary ) {
		_body = body;
		_parser = new MultiPart.Parser( boundary, new Listener() );
	}

	@Override
	public boolean hasNext() {
		if( !_pending.isEmpty() ) {
			return true;
		}

		// Skip whatever's left of the part handed over last
		if( _handedOver != null ) {
			_handedOver.close();

			while( !_handedOver._ended ) {
				parseMore();
			}
		}

		while( _pending.isEmpty() && !_complete ) {
			parseMore();
		}

		return !_pending.isEmpty();
	}

	@Override
	public Part next() {
		if( !hasNext() ) {
			throw new NoSuchElementException();
		}

		final Part part = _pending.poll();
		_handedOver = (PartContent)part.stream();
		return part;
	}

	/**
	 * Passes the next piece of the body to the parser
	 */
	private void parseMore() {
		if( _failure != null ) {
			throw new IllegalStateException( "The multipart body couldn't be parsed", _failure );
		}

		if( _bodyEnded ) {
			throw new IllegalStateException( "The multipart body ended before its closing boundary" );
		}

		try {
			final byte[] buffer = new byte[8192];
			final int read = _body.read( buffer );

			if( read == -1 ) {
				_bodyEnded = true;
				_parser.parse( Content.Chunk.EOF );
			}
			else {
				_parser.parse( Content.Chunk.from( ByteBuffer.wrap( buffer, 0, read ), false ) );
			}
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}

		if( _failure != null ) {
			throw new IllegalStateException( "The multipart body couldn't be parsed", _failure );
		}
	}

	/**
	 * Receives the parser's events. Jetty's AbstractPartsListener reads the part's name and filename from its Content-Disposition header.
	 */
	private class Listener extends MultiPart.AbstractPartsListener {

		private Map<String, List<String>> _headers;

		@Override
		public void onPartBegin() {
			_headers = NGMessage.createEmptyHeadersMap();
			_parsing = new PartContent();
		}

		@Override
		public void onPartHeader( final String name, final String value ) {
			super.onPartHeader( name, value );
			_headers.computeIfAbsent( name, _unused -> new ArrayList<>() ).add( value );
		}

		@Override
		public void onPartHeaders() {
			_pending.add( new Part( getName(), getFileName(), _headers, _parsing, -1 ) );
		}

		@Override
		public void onPartContent( final Content.Chunk chunk ) {
			_parsing.add( chunk.getByteBuffer() );
		}

		@Override
		public void onPartEnd() {
			super.onPartEnd();
			_parsing._ended = true;
		}

		@Override
		public void onPart( final String name, final String fileName, final HttpFields headers ) {}

		@Override
		public void onComplete() {
			_complete = true;
		}

		@Override
		public void onFailure( final Throwable failure ) {
			_failure = failure;
		}
	}

	/**
	 * A part's content, filled by the parser as the body is read
	 */
	private class PartContent extends InputStream {

		private final Deque<byte[]> _chunks = new ArrayDeque<>();
		private int _position;
		private boolean _ended;
		private boolean _closed;

		void add( final ByteBuffer buffer ) {
			if( !_closed && buffer.hasRemaining() ) {
				final byte[] bytes = new byte[buffer.remaining()];
				buffer.get( bytes );
				_chunks.add( bytes );
			}
		}

		/**
		 * @return true if there's content to read, false at the part's end
		 */
		private boolean ensureContent() throws IOException {
			if( _closed ) {
				throw new IOException( "This part's content is no longer readable, since the next part has been asked for" );
			}

			while( _chunks.isEmpty() && !_ended ) {
				parseMore();
			}

			return !_chunks.isEmpty();
		}

		@Override
		public int read() throws IOException {
			if( !ensureContent() ) {
				return -1;
			}

			final byte[] chunk = _chunks.peek();
			final int b = chunk[_position++] & 0xFF;

			if( _position == chunk.length ) {
				_chunks.poll();
				_position = 0;
			}

			return b;
		}

		@Override
		public int read( final byte[] b, final int off, final int len ) throws IOException {
			if( len == 0 ) {
				return 0;
			}

			if( !ensureContent() ) {
				return -1;
			}

			final byte[] chunk = _chunks.peek();
			final int count = Math.min( len, chunk.length - _position );
			System.arraycopy( chunk, _position, b, off, count );
			_position += count;

			if( _position == chunk.length ) {
				_chunks.poll();
				_position = 0;
			}

			return count;
		}

		@Override
		public void close() {
			_closed = true;
			_chunks.clear();
		}
	}
}
