package ng.appserver.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ng.appserver.http.NGMultipartRequest.Part;


public class TestNGMultipartRequest {

	private static Part field( final String name, final String value ) {
		return field( name, value, null, StandardCharsets.UTF_8 );
	}

	private static Part field( final String name, final String value, final String contentType, final Charset charset ) {
		final byte[] bytes = value.getBytes( charset );
		final Map<String, List<String>> headers = contentType == null ? Map.of() : Map.of( "Content-Type", List.of( contentType ) );
		return new Part( name, null, headers, () -> new ByteArrayInputStream( bytes ), (long)bytes.length );
	}

	private static Part file( final String name, final String filename, final String content ) {
		final byte[] bytes = content.getBytes( StandardCharsets.UTF_8 );
		return new Part( name, filename, Map.of( "Content-Type", List.of( "application/octet-stream" ) ), () -> new ByteArrayInputStream( bytes ), (long)bytes.length );
	}

	private static NGRequestLimits limits( final int maxParts, final Long maxMultipartContentLength, final int maxMemoryPartLength, final Path temporaryDirectory ) {
		return new NGRequestLimits( 1000, 200_000, maxParts, maxMultipartContentLength, maxMemoryPartLength, temporaryDirectory );
	}

	/**
	 * A request with the given query string and parts, counting how often its body is parsed (to collect the parts, or hand them over as they arrive)
	 */
	private static NGMultipartRequest request( final String queryString, final AtomicInteger reads, final Part... parts ) {
		return request( queryString, reads, NGRequestLimits.DEFAULT, parts );
	}

	private static NGMultipartRequest request( final String queryString, final AtomicInteger reads, final NGRequestLimits limits, final Part... parts ) {
		final Map<String, List<String>> headers = Map.of( "Content-Type", List.of( "multipart/form-data; boundary=x" ) );
		final NGMultipartParser parser = ( body, boundary ) -> {
			reads.incrementAndGet();
			return List.of( parts ).iterator();
		};

		return new NGMultipartRequest( "POST", "/", queryString, headers, InputStream.nullInputStream(), "x", parser, limits, null );
	}

	private static NGMultipartRequest request( final Part... parts ) {
		return request( null, new AtomicInteger(), parts );
	}

	@Test
	public void partsArentReadUntilNeeded() {
		final AtomicInteger reads = new AtomicInteger();
		final NGMultipartRequest request = request( null, reads, field( "a", "1" ) );
		request.uri();
		request.headers();
		request.toString();
		assertEquals( 0, reads.get() );
	}

	@Test
	public void partsAreReadOnce() {
		final AtomicInteger reads = new AtomicInteger();
		final NGMultipartRequest request = request( null, reads, field( "a", "1" ), file( "f", "f.txt", "x" ) );
		request.formValues();
		request.uploadedFiles();
		request.formValueForKey( "a" );
		assertEquals( 1, reads.get() );
	}

	@Test
	public void fieldsAreFormValues() {
		assertEquals( Map.of( "a", List.of( "1", "3" ), "b", List.of( "2" ) ), request( field( "a", "1" ), field( "b", "2" ), field( "a", "3" ) ).formValues() );
	}

	@Test
	public void queryStringValuesComeBeforeTheFields() {
		final NGMultipartRequest request = request( "a=query&q=1", new AtomicInteger(), field( "a", "field" ) );
		assertEquals( List.of( "query", "field" ), request.formValuesForKey( "a" ) );
		assertEquals( "1", request.formValueForKey( "q" ) );
	}

	@Test
	public void filesArentFormValues() {
		final NGMultipartRequest request = request( field( "a", "1" ), file( "f", "f.txt", "x" ) );
		assertEquals( Map.of( "a", List.of( "1" ) ), request.formValues() );
	}

	@Test
	public void fieldsArentFiles() {
		final NGMultipartRequest request = request( field( "a", "1" ), file( "f", "f.txt", "x" ) );
		assertEquals( List.of( "f" ), List.copyOf( request.uploadedFiles().keySet() ) );
	}

	@Test
	public void aPartWithAContentTypeButNoFilenameIsAField() {
		assertEquals( "1", request( field( "a", "1", "text/plain", StandardCharsets.UTF_8 ) ).formValueForKey( "a" ) );
	}

	@Test
	public void fieldCharset() {
		assertEquals( "Þórður", request( field( "nafn", "Þórður", "text/plain; charset=ISO-8859-1", StandardCharsets.ISO_8859_1 ) ).formValueForKey( "nafn" ) );
		assertEquals( "Þórður", request( field( "nafn", "Þórður" ) ).formValueForKey( "nafn" ) );
	}

	@Test
	public void uploadedFile() throws IOException {
		final Part file = request( file( "f", "notes.txt", "contents" ) ).uploadedFileForKey( "f" );
		assertEquals( "notes.txt", file.filename() );
		assertEquals( "application/octet-stream", file.contentType() );
		assertEquals( 8, file.length() );
		assertEquals( "contents", new String( file.stream().readAllBytes(), StandardCharsets.UTF_8 ) );
	}

	@Test
	public void aFileFieldSubmittedWithoutAFileIsAnEmptyFile() {
		final Part file = request( file( "f", "", "" ) ).uploadedFileForKey( "f" );
		assertEquals( "", file.filename() );
		assertEquals( 0, file.length() );
	}

	@Test
	public void manyFilesWithOneField() {
		final NGMultipartRequest request = request( file( "f", "a.txt", "a" ), file( "f", "b.txt", "b" ) );
		assertEquals( List.of( "a.txt", "b.txt" ), request.uploadedFilesForKey( "f" ).stream().map( Part::filename ).toList() );
		assertEquals( "a.txt", request.uploadedFileForKey( "f" ).filename() );
	}

	@Test
	public void noSuchFile() {
		final NGMultipartRequest request = request( field( "a", "1" ) );
		assertNull( request.uploadedFileForKey( "f" ) );
		assertTrue( request.uploadedFilesForKey( "f" ).isEmpty() );
	}

	@Test
	public void describingTheBodyDoesntReadIt() {
		final AtomicInteger reads = new AtomicInteger();
		final NGMultipartRequest request = request( null, reads, field( "a", "1" ) );
		assertEquals( "multipart/form-data; boundary=x", request.contentType() );
		assertNull( request.contentLength() );
		assertEquals( 0, reads.get() );
	}


	@Test
	public void partHeadersAreCaseInsensitive() {
		final Part part = new Part( "a", null, Map.of( "X-Checksum", List.of( "abc" ), "content-type", List.of( "text/plain" ) ), () -> new ByteArrayInputStream( new byte[0] ), 0L );
		assertEquals( List.of( "abc" ), part.headers().get( "x-checksum" ) );
		assertEquals( "text/plain", part.contentType() );
	}

	@Test
	public void incomingParts() {
		final AtomicInteger reads = new AtomicInteger();
		final NGMultipartRequest request = request( null, reads, field( "a", "1" ), file( "f", "f.txt", "x" ) );
		final List<String> names = new ArrayList<>();

		for( final Part part : request.incomingParts() ) {
			names.add( part.name() + ":" + part.isFile() );
		}

		assertEquals( List.of( "a:false", "f:true" ), names );
		assertEquals( 1, reads.get() );
	}

	@Test
	public void incomingPartsCanBeIteratedOnce() {
		final Iterable<Part> parts = request( field( "a", "1" ) ).incomingParts();
		parts.iterator();
		assertThrows( IllegalStateException.class, parts::iterator );
	}

	@Test
	public void incomingPartsCanBeAskedForOnce() {
		final NGMultipartRequest request = request( field( "a", "1" ) );
		request.incomingParts();
		assertThrows( IllegalStateException.class, request::incomingParts );
	}

	@Test
	public void afterIncomingPartsFormValuesAreTheQueryStrings() {
		final NGMultipartRequest request = request( "folder=x", new AtomicInteger(), field( "a", "1" ) );
		request.incomingParts();
		assertEquals( Map.of( "folder", List.of( "x" ) ), request.formValues() );
	}

	@Test
	public void afterIncomingPartsUploadedFilesAreUnavailable() {
		final NGMultipartRequest request = request( file( "f", "f.txt", "x" ) );
		request.incomingParts();
		assertThrows( IllegalStateException.class, request::uploadedFiles );
	}

	@Test
	public void afterCollectingIncomingPartsAreTheCollectedOnes() throws IOException {
		final NGMultipartRequest request = request( field( "a", "1" ), file( "f", "f.txt", "x" ) );
		assertEquals( "1", request.formValueForKey( "a" ) );

		final List<String> contents = new ArrayList<>();

		for( final Part part : request.incomingParts() ) {
			contents.add( new String( part.stream().readAllBytes(), StandardCharsets.UTF_8 ) );
		}

		// The field was already read for form values, and can still be read as a part
		assertEquals( List.of( "1", "x" ), contents );
		assertEquals( "x", new String( request.uploadedFileForKey( "f" ).stream().readAllBytes(), StandardCharsets.UTF_8 ) );
	}

	@Test
	public void afterCollectingTheRawBodyIsntKept() {
		final NGMultipartRequest request = request( field( "a", "1" ) );
		request.formValues();
		final IllegalStateException e = assertThrows( IllegalStateException.class, request::contentBytes );
		assertTrue( e.getMessage().contains( "formValues() or uploadedFiles()" ) );
		assertThrows( IllegalStateException.class, request::contentStream );
	}

	/**
	 * A request whose "parser" reads the whole body as a single field named "body", so tests can tell whether parts come from the body
	 */
	private static NGMultipartRequest requestWithBody( final String body ) {
		final Map<String, List<String>> headers = Map.of( "Content-Type", List.of( "multipart/form-data; boundary=x" ) );
		final NGMultipartParser parser = ( in, boundary ) -> {
			try {
				return List.of( field( "body", new String( in.readAllBytes(), StandardCharsets.UTF_8 ) ) ).iterator();
			}
			catch( IOException e ) {
				throw new UncheckedIOException( e );
			}
		};

		return new NGMultipartRequest( "POST", "/", "q=1", headers, new ByteArrayInputStream( body.getBytes( StandardCharsets.UTF_8 ) ), "x", parser, NGRequestLimits.DEFAULT, null );
	}

	@Test
	public void theRawBodyIsTheBodyAsItArrived() {
		assertEquals( "--x raw", requestWithBody( "--x raw" ).contentString() );
	}

	@Test
	public void afterReadingTheRawBodyThePartsComeFromWhatsKept() {
		final NGMultipartRequest request = requestWithBody( "--x raw" );
		assertEquals( "--x raw", request.contentString() );
		assertEquals( "--x raw", request.formValueForKey( "body" ) );
		assertEquals( "--x raw", request.contentString() );
	}

	@Test
	public void afterReadingTheRawBodyIncomingPartsComeFromWhatsKept() throws IOException {
		final NGMultipartRequest request = requestWithBody( "--x raw" );
		request.contentBytes();
		assertEquals( "--x raw", new String( request.incomingParts().iterator().next().stream().readAllBytes(), StandardCharsets.UTF_8 ) );
		assertEquals( "--x raw", request.contentString() );
	}

	@Test
	public void afterStreamingTheRawBodyOnlyTheQueryStringRemains() throws IOException {
		final NGMultipartRequest request = requestWithBody( "--x raw" );
		assertEquals( "--x raw", new String( request.contentStream().readAllBytes(), StandardCharsets.UTF_8 ) );
		assertEquals( Map.of( "q", List.of( "1" ) ), request.formValues() );
		final IllegalStateException e = assertThrows( IllegalStateException.class, request::contentBytes );
		assertTrue( e.getMessage().contains( "contentStream()" ) );
		assertThrows( IllegalStateException.class, request::uploadedFiles );
		assertThrows( IllegalStateException.class, request::incomingParts );
	}

	@Test
	public void afterIncomingPartsTheRawBodyIsGone() {
		final NGMultipartRequest request = requestWithBody( "--x raw" );
		request.incomingParts().iterator().next();
		final IllegalStateException e = assertThrows( IllegalStateException.class, request::contentString );
		assertTrue( e.getMessage().contains( "incomingParts()" ) );
	}

	private static Part bytes( final String name, final String filename, final int length ) {
		return new Part( name, filename, Map.of(), () -> new ByteArrayInputStream( new byte[length] ), null );
	}

	@Test
	public void collectedPartsHaveTheirLength() {
		final NGMultipartRequest request = request( bytes( "f", "a.bin", 1234 ) );
		assertEquals( 1234, request.uploadedFileForKey( "f" ).length() );
	}

	@Test
	public void largePartsAreKeptInTemporaryFilesUntilTheRequestIsClosed( @TempDir final Path directory ) throws IOException {
		final NGRequestLimits limits = limits( 1000, null, 100, directory );
		final byte[] content = "x".repeat( 1000 ).getBytes( StandardCharsets.UTF_8 );
		final Part large = new Part( "f", "large.txt", Map.of(), () -> new ByteArrayInputStream( content ), null );
		final NGMultipartRequest request = request( null, new AtomicInteger(), limits, large, bytes( "small", "small.bin", 50 ) );

		final Part kept = request.uploadedFileForKey( "f" );

		try( var files = Files.list( directory ) ) {
			assertEquals( 1, files.count(), "Only the part longer than maxMemoryPartLength should be in a temporary file" );
		}

		assertEquals( 1000, kept.length() );
		assertArrayEquals( content, kept.stream().readAllBytes() );

		request._close();

		try( var files = Files.list( directory ) ) {
			assertEquals( 0, files.count() );
		}
	}

	@Test
	public void partLimit() {
		final NGRequestLimits limits = limits( 2, null, 1000, NGRequestLimits.DEFAULT.temporaryDirectory() );
		assertEquals( 2, request( null, new AtomicInteger(), limits, field( "a", "1" ), field( "b", "2" ) ).formValues().size() );
		assertThrows( IllegalArgumentException.class, () -> request( null, new AtomicInteger(), limits, field( "a", "1" ), field( "b", "2" ), field( "c", "3" ) ).formValues() );
	}

	@Test
	public void contentLengthLimitCountsEveryPart( @TempDir final Path directory ) throws IOException {
		final NGRequestLimits limits = limits( 1000, 100L, 10, directory );
		final NGMultipartRequest withinLimit = request( null, new AtomicInteger(), limits, bytes( "a", "a", 60 ), bytes( "b", "b", 40 ) );
		assertEquals( 100, withinLimit.uploadedFiles().values().stream().flatMap( List::stream ).mapToLong( Part::length ).sum() );
		withinLimit._close();

		final NGMultipartRequest tooLong = request( null, new AtomicInteger(), limits, bytes( "a", "a", 60 ), bytes( "b", "b", 41 ) );
		assertThrows( IllegalArgumentException.class, tooLong::uploadedFiles );

		// What was kept before the limit was hit is still cleaned up
		tooLong._close();

		try( var files = Files.list( directory ) ) {
			assertEquals( 0, files.count() );
		}
	}

	@Test
	public void incomingPartsArentLimitedOrKept( @TempDir final Path directory ) throws IOException {
		final NGRequestLimits limits = limits( 1, 10L, 10, directory );
		int count = 0;

		for( final Part part : request( null, new AtomicInteger(), limits, bytes( "a", "a", 100 ), bytes( "b", "b", 100 ) ).incomingParts() ) {
			assertEquals( 100, part.stream().readAllBytes().length );
			count++;
		}

		assertEquals( 2, count );

		try( var files = Files.list( directory ) ) {
			assertEquals( 0, files.count() );
		}
	}

	@Test
	public void requestsOfMultipartContentAreMultipartRequests() {
		final NGMultipartParser parser = ( body, boundary ) -> List.<Part> of().iterator();
		final Map<String, List<String>> multipart = Map.of( "content-type", List.of( "multipart/form-data; boundary=\"abc=def\"" ) );
		assertTrue( NGRequests.of( "POST", "/", null, multipart, InputStream.nullInputStream(), null, parser ) instanceof NGMultipartRequest );

		// No parser, no boundary, or another content type: the body is left as it is
		assertFalse( NGRequests.of( "POST", "/", null, multipart, InputStream.nullInputStream(), null, null ) instanceof NGMultipartRequest );
		assertFalse( NGRequests.of( "POST", "/", null, Map.of( "Content-Type", List.of( "multipart/form-data" ) ), InputStream.nullInputStream(), null, parser ) instanceof NGMultipartRequest );
		assertFalse( NGRequests.of( "POST", "/", null, Map.of( "Content-Type", List.of( "text/plain" ) ), InputStream.nullInputStream(), null, parser ) instanceof NGMultipartRequest );
		assertTrue( NGRequests.of( "GET", "/", null, Map.of(), InputStream.nullInputStream(), null, parser ) instanceof NGStandardRequest );
	}

	@Test
	public void theBoundaryComesFromTheContentType() {
		final AtomicInteger boundaries = new AtomicInteger();
		final NGMultipartParser parser = ( body, boundary ) -> {
			assertEquals( "abc=def", boundary );
			boundaries.incrementAndGet();
			return List.<Part> of().iterator();
		};

		final NGRequest request = NGRequests.of( "POST", "/", null, Map.of( "Content-Type", List.of( "Multipart/Form-Data; charset=utf-8; boundary=\"abc=def\"" ) ), InputStream.nullInputStream(), null, parser );
		request.formValues();
		assertEquals( 1, boundaries.get() );
	}

	@Test
	public void anUploadedFileCanBeReadMoreThanOnce() throws IOException {
		final NGMultipartRequest request = request( file( "f", "f.txt", "contents" ) );
		final Part file = request.uploadedFileForKey( "f" );
		assertEquals( "contents", new String( file.stream().readAllBytes(), StandardCharsets.UTF_8 ) );
		assertEquals( "contents", new String( file.stream().readAllBytes(), StandardCharsets.UTF_8 ) );
		assertEquals( "contents", new String( request.uploadedFileForKey( "f" ).stream().readAllBytes(), StandardCharsets.UTF_8 ) );
	}

	@Test
	public void aLargeUploadedFileCanBeReadMoreThanOnce( @TempDir final Path directory ) throws IOException {
		final byte[] content = "x".repeat( 1000 ).getBytes( StandardCharsets.UTF_8 );
		final NGMultipartRequest request = request( null, new AtomicInteger(), limits( 1000, null, 100, directory ), new Part( "f", "big.txt", Map.of(), () -> new ByteArrayInputStream( content ), null ) );
		final Part file = request.uploadedFileForKey( "f" );
		assertArrayEquals( content, file.stream().readAllBytes() );
		assertArrayEquals( content, file.stream().readAllBytes() );
		request._close();
	}

	@Test
	public void partsOverTheLimitAreRefusedFromThenOn( @TempDir final Path directory ) {
		final NGMultipartRequest request = request( null, new AtomicInteger(), limits( 1000, 10L, 100, directory ), bytes( "f", "big.bin", 100 ) );
		assertThrows( IllegalArgumentException.class, request::uploadedFiles );

		// Collecting failed partway, so the parts are gone. Later reads say why, instead of returning the query string's form values as if the parts had been read as a stream
		final IllegalStateException e = assertThrows( IllegalStateException.class, request::formValues );
		assertTrue( e.getMessage().contains( "collecting its parts failed" ) );
		assertThrows( IllegalStateException.class, request::uploadedFiles );
		request._close();
	}
}
