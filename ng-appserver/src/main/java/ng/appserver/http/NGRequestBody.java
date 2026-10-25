package ng.appserver.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * A request's body, and the rule for reading it: the body arrives once, so reading it in full keeps it (and later reads are served from what's kept), while reading it as a stream keeps nothing.
 */

final class NGRequestBody {

	/**
	 * The body as received from the adaptor. Readable only while the adaptor is handling the request.
	 */
	private final InputStream _stream;

	/**
	 * The body, once read in full. null until then.
	 */
	private byte[] _bytes;

	/**
	 * What read the body as a stream, for explaining why it can't be read again. null if nothing has.
	 */
	private String _streamedBy;

	/**
	 * Why reading the body failed, for explaining why it can't be read again. null if it hasn't.
	 */
	private String _failure;

	NGRequestBody( final InputStream stream ) {
		Objects.requireNonNull( stream );
		_stream = stream;
	}

	/**
	 * @return true if the body has been read in full and kept
	 */
	synchronized boolean isKept() {
		return _bytes != null;
	}

	/**
	 * @return true if the body has been handed over as a stream, so it's gone. false if reading it failed (see requireReadable()).
	 */
	synchronized boolean isStreamed() {
		return _streamedBy != null;
	}

	/**
	 * @return The body, read in full and kept on first invocation
	 * @throws IllegalArgumentException If the body has to be read and turns out longer than maxLength bytes (null for no limit)
	 * @throws IllegalStateException If the body has been handed over as a stream, or reading it failed before
	 */
	synchronized byte[] bytes( final Long maxLength ) {
		if( _bytes == null ) {
			requireReadable( "read in full" );
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

			try( _stream ) {
				final byte[] buffer = new byte[8192];
				long length = 0;
				int read;

				while( (read = _stream.read( buffer )) != -1 ) {
					length += read;

					if( maxLength != null && length > maxLength ) {
						fail( "it turned out longer than %s bytes".formatted( maxLength ) );
						throw new IllegalArgumentException( "Content is longer than %s bytes".formatted( maxLength ) );
					}

					bytes.write( buffer, 0, read );
				}
			}
			catch( IOException e ) {
				fail( "it couldn't be read (%s)".formatted( e.getMessage() ) );
				throw new UncheckedIOException( e );
			}

			_bytes = bytes.toByteArray();
		}

		return _bytes;
	}

	/**
	 * @param reader What's reading the body, for explaining later reads that can't be served
	 * @return The body as a stream: over the kept bytes if it's been read in full, otherwise the body itself, which is then gone
	 * @throws IllegalStateException If the body has already been handed over as a stream, or reading it failed before
	 */
	synchronized InputStream stream( final String reader ) {
		if( _bytes != null ) {
			return new ByteArrayInputStream( _bytes );
		}

		requireReadable( "read as a stream" );
		_streamedBy = reader;
		return _stream;
	}

	/**
	 * Records that reading the body failed partway, so later reads refuse rather than find it seemingly untouched
	 */
	synchronized void fail( final String reason ) {
		if( _failure == null ) {
			_failure = reason;
		}
	}

	/**
	 * @throws IllegalStateException If reading the body failed before, explaining that the read attempted can't be served
	 */
	synchronized void requireNotFailed( final String attempted ) {
		if( _failure != null ) {
			throw new IllegalStateException( "This request's body can't be %s, since reading it failed before: %s".formatted( attempted, _failure ) );
		}
	}

	/**
	 * @throws IllegalStateException If the body has been handed over as a stream, or reading it failed before, explaining that the read attempted can't be served
	 */
	synchronized void requireReadable( final String attempted ) {
		requireNotFailed( attempted );

		if( _streamedBy != null ) {
			throw new IllegalStateException( "This request's body can't be %s, since it was already read by %s, which doesn't keep it. To read a body more than once, read it first with contentBytes() or contentString(), which keep it".formatted( attempted, _streamedBy ) );
		}
	}
}
