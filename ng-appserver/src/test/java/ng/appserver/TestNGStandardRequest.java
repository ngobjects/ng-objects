package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGStandardRequest;

public class TestNGStandardRequest {

	/**
	 * A body stream that records whether it's been read or closed
	 */
	private static class RecordingStream extends ByteArrayInputStream {

		boolean read;
		boolean closed;

		RecordingStream( final String content ) {
			super( content.getBytes( StandardCharsets.UTF_8 ) );
		}

		@Override
		public synchronized int read() {
			read = true;
			return super.read();
		}

		@Override
		public synchronized int read( byte[] b, int off, int len ) {
			read = true;
			return super.read( b, off, len );
		}

		@Override
		public long transferTo( OutputStream out ) throws IOException {
			read = true;
			return super.transferTo( out );
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}

	private static NGStandardRequest request( final InputStream contentStream ) {
		return new NGStandardRequest( "POST", "/", "HTTP/1.1", Map.of(), Map.of(), Map.of(), contentStream, null );
	}

	@Test
	public void contentIsNotReadAtConstruction() {
		final RecordingStream stream = new RecordingStream( "body" );
		request( stream );
		assertFalse( stream.read );
		assertFalse( stream.closed );
	}

	@Test
	public void contentIsReadOnFirstAccessAndKept() {
		final RecordingStream stream = new RecordingStream( "body" );
		final NGStandardRequest request = request( stream );
		assertEquals( "body", request.contentString() );
		assertTrue( stream.closed );
		assertEquals( "body", request.contentString() );
		assertEquals( 4, request.contentBytes().length );
	}

	@Test
	public void emptyContent() {
		assertEquals( "", request( InputStream.nullInputStream() ).contentString() );
	}
}
