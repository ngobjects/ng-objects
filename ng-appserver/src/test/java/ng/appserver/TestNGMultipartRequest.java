package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGMultipartReader;
import ng.appserver.http.NGMultipartRequest;
import ng.appserver.http.NGMultipartRequest.Part;

public class TestNGMultipartRequest {

	private static Part field( final String name, final String value ) {
		return field( name, value, null, StandardCharsets.UTF_8 );
	}

	private static Part field( final String name, final String value, final String contentType, final Charset charset ) {
		final byte[] bytes = value.getBytes( charset );
		final Map<String, List<String>> headers = contentType == null ? Map.of() : Map.of( "Content-Type", List.of( contentType ) );
		return new Part( name, null, headers, new ByteArrayInputStream( bytes ), bytes.length );
	}

	private static Part file( final String name, final String filename, final String content ) {
		final byte[] bytes = content.getBytes( StandardCharsets.UTF_8 );
		return new Part( name, filename, Map.of( "Content-Type", List.of( "application/octet-stream" ) ), new ByteArrayInputStream( bytes ), bytes.length );
	}

	/**
	 * A request with the given query string and parts, counting how often the parts are read (collected or incoming)
	 */
	private static NGMultipartRequest request( final String queryString, final AtomicInteger reads, final Part... parts ) {
		final Map<String, List<String>> headers = Map.of( "Content-Type", List.of( "multipart/form-data; boundary=x" ) );
		final NGMultipartReader reader = new NGMultipartReader() {

			@Override
			public List<Part> readAll() {
				reads.incrementAndGet();
				return List.of( parts );
			}

			@Override
			public Iterator<Part> iterate() {
				reads.incrementAndGet();
				return List.of( parts ).iterator();
			}
		};

		return new NGMultipartRequest( "POST", "/", queryString, "HTTP/1.1", headers, Map.of(), reader, null );
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
		assertThrows( IllegalStateException.class, () -> request.uploadedFileForKey( "f" ) );
	}

	@Test
	public void noSuchFile() {
		final NGMultipartRequest request = request( field( "a", "1" ) );
		assertNull( request.uploadedFileForKey( "f" ) );
		assertTrue( request.uploadedFilesForKey( "f" ).isEmpty() );
	}

	@Test
	public void contentIsTheParts() {
		final AtomicInteger reads = new AtomicInteger();
		final NGMultipartRequest request = request( null, reads, field( "a", "1" ) );
		assertThrows( UnsupportedOperationException.class, request::contentString );
		assertEquals( 0, reads.get() );
	}


	@Test
	public void partHeadersAreCaseInsensitive() {
		final Part part = new Part( "a", null, Map.of( "X-Checksum", List.of( "abc" ), "content-type", List.of( "text/plain" ) ), new ByteArrayInputStream( new byte[0] ), 0 );
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
	public void afterCollectingIncomingPartsAreUnavailable() {
		final NGMultipartRequest request = request( field( "a", "1" ) );
		request.formValues();
		assertThrows( IllegalStateException.class, request::incomingParts );
	}
}
