package ng.adaptor.jetty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGMultipartRequest.Part;

public class TestNGIncomingParts {

	private static final String BOUNDARY = "----ngboundary";

	/**
	 * Builds a multipart body
	 */
	private static class Body {

		private final ByteArrayOutputStream _out = new ByteArrayOutputStream();

		Body field( final String name, final String value ) {
			return part( "form-data; name=\"" + name + "\"", null, value.getBytes( StandardCharsets.UTF_8 ) );
		}

		Body file( final String name, final String filename, final byte[] content ) {
			return part( "form-data; name=\"" + name + "\"; filename=\"" + filename + "\"", "Content-Type: application/octet-stream\r\n", content );
		}

		Body part( final String disposition, final String extraHeaders, final byte[] content ) {
			write( "--" + BOUNDARY + "\r\n" );
			write( "Content-Disposition: " + disposition + "\r\n" );

			if( extraHeaders != null ) {
				write( extraHeaders );
			}

			write( "\r\n" );
			_out.writeBytes( content );
			write( "\r\n" );
			return this;
		}

		byte[] end() {
			write( "--" + BOUNDARY + "--\r\n" );
			return _out.toByteArray();
		}

		private void write( final String string ) {
			_out.writeBytes( string.getBytes( StandardCharsets.UTF_8 ) );
		}
	}

	/**
	 * A stream handing over the given bytes a few at a time, as a slow connection would, counting how many it has handed over
	 */
	private static class TricklingStream extends InputStream {

		private final byte[] _bytes;
		private final Random _random = new Random( 42 );
		int position;

		TricklingStream( final byte[] bytes ) {
			_bytes = bytes;
		}

		@Override
		public int read() {
			return position < _bytes.length ? _bytes[position++] & 0xFF : -1;
		}

		@Override
		public int read( final byte[] b, final int off, final int len ) {
			if( position == _bytes.length ) {
				return -1;
			}

			final int count = Math.min( Math.min( len, 1 + _random.nextInt( 7 ) ), _bytes.length - position );
			System.arraycopy( _bytes, position, b, off, count );
			position += count;
			return count;
		}
	}

	private static NGIncomingParts parts( final byte[] body ) {
		return new NGIncomingParts( new ByteArrayInputStream( body ), BOUNDARY );
	}

	private static String content( final Part part ) throws IOException {
		return new String( part.stream().readAllBytes(), StandardCharsets.UTF_8 );
	}

	@Test
	public void fieldsAndFiles() throws IOException {
		final NGIncomingParts parts = parts( new Body().field( "a", "1" ).file( "f", "notes.txt", "contents".getBytes( StandardCharsets.UTF_8 ) ).field( "b", "Þórður" ).end() );

		final Part a = parts.next();
		assertEquals( "a", a.name() );
		assertNull( a.filename() );
		assertFalse( a.isFile() );
		assertEquals( -1, a.length() );
		assertEquals( "1", content( a ) );

		final Part f = parts.next();
		assertEquals( "f", f.name() );
		assertEquals( "notes.txt", f.filename() );
		assertEquals( "application/octet-stream", f.contentType() );
		assertEquals( "contents", content( f ) );

		final Part b = parts.next();
		assertEquals( "Þórður", content( b ) );

		assertFalse( parts.hasNext() );
	}

	@Test
	public void partHeaders() {
		final byte[] body = new Body().part( "form-data; name=\"f\"; filename=\"a.bin\"", "Content-Type: image/png\r\nX-Checksum: abc123\r\n", new byte[] { 1, 2, 3 } ).end();
		final Part part = parts( body ).next();
		assertEquals( List.of( "abc123" ), part.headers().get( "x-checksum" ) );
		assertEquals( "image/png", part.contentType() );
	}

	@Test
	public void encodedFilename() {
		final byte[] body = new Body().part( "form-data; name=\"f\"; filename*=UTF-8''%C3%9E%C3%B3r.txt", null, new byte[0] ).end();
		assertEquals( "Þór.txt", parts( body ).next().filename() );
	}

	@Test
	public void fileFieldWithoutAFile() throws IOException {
		final Part part = parts( new Body().file( "f", "", new byte[0] ).end() ).next();
		assertEquals( "", part.filename() );
		assertTrue( part.isFile() );
		assertEquals( "", content( part ) );
	}

	@Test
	public void noParts() {
		assertFalse( parts( ("--" + BOUNDARY + "--\r\n").getBytes( StandardCharsets.UTF_8 ) ).hasNext() );
	}

	@Test
	public void unreadPartsAreSkipped() throws IOException {
		final NGIncomingParts parts = parts( new Body().field( "a", "1" ).file( "f", "f.txt", "x".repeat( 100_000 ).getBytes( StandardCharsets.UTF_8 ) ).field( "b", "2" ).end() );
		assertEquals( "a", parts.next().name() );
		assertEquals( "f", parts.next().name() );
		assertEquals( "2", content( parts.next() ) );
	}

	@Test
	public void aPartReadHalfwayIsSkippedAndNoLongerReadable() throws IOException {
		final NGIncomingParts parts = parts( new Body().file( "f", "f.txt", "x".repeat( 100_000 ).getBytes( StandardCharsets.UTF_8 ) ).field( "b", "2" ).end() );
		final Part f = parts.next();
		f.stream().readNBytes( 10 );
		assertEquals( "2", content( parts.next() ) );
		assertThrows( IOException.class, () -> f.stream().read() );
	}

	@Test
	public void contentSurvivesBeingSplitAnywhere() throws IOException {
		final byte[] content = new byte[300_000];
		new Random( 7 ).nextBytes( content );

		// Sprinkle the content with things that look like the start of a boundary
		final byte[] lookalike = ("\r\n--" + BOUNDARY.substring( 0, 8 )).getBytes( StandardCharsets.UTF_8 );
		for( int i = 1000; i < content.length - lookalike.length; i += 9973 ) {
			System.arraycopy( lookalike, 0, content, i, lookalike.length );
		}

		final byte[] body = new Body().field( "a", "1" ).file( "f", "random.bin", content ).field( "b", "2" ).end();
		final NGIncomingParts parts = new NGIncomingParts( new TricklingStream( body ), BOUNDARY );

		assertEquals( "1", content( parts.next() ) );
		assertArrayEquals( content, parts.next().stream().readAllBytes() );
		assertEquals( "2", content( parts.next() ) );
		assertFalse( parts.hasNext() );
	}

	@Test
	public void partsAreReadFromTheBodyAsTheyreAskedFor() throws IOException {
		final byte[] big = new byte[1_000_000];
		final byte[] body = new Body().field( "a", "1" ).file( "f", "big.bin", big ).end();
		final TricklingStream stream = new TricklingStream( body );
		final NGIncomingParts parts = new NGIncomingParts( stream, BOUNDARY );

		assertEquals( "1", content( parts.next() ) );
		final Part f = parts.next();
		assertTrue( stream.position < 10_000, "Handing over the file shouldn't read it, but " + stream.position + " bytes were read" );

		f.stream().readNBytes( 500_000 );
		assertTrue( stream.position < 600_000, "Reading half the file shouldn't read all of it, but " + stream.position + " bytes were read" );
	}

	@Test
	public void aTruncatedBodyFails() {
		final byte[] whole = new Body().field( "a", "1" ).file( "f", "f.txt", "contents".getBytes( StandardCharsets.UTF_8 ) ).end();
		final byte[] truncated = Arrays.copyOf( whole, whole.length - 30 );
		final NGIncomingParts parts = parts( truncated );
		parts.next();
		assertThrows( RuntimeException.class, () -> {
			final Part f = parts.next();
			f.stream().readAllBytes();
			parts.hasNext();
		} );
	}
}
