package ng.appserver.templating.elements;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.NGComponentRequestTestHarness.TestContext;
import ng.appserver.NGComponentRequestTestHarness.TestPage;
import ng.appserver.NGComponentRequestTestHarness.TestRequest;
import ng.appserver.NGContext;
import ng.appserver.NGSession;
import ng.appserver.http.NGMultipartParser;
import ng.appserver.http.NGMultipartRequest;
import ng.appserver.http.NGMultipartRequest.Part;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGRequests;
import ng.appserver.http.NGResponses;
import ng.appserver.http.NGStandardResponse;
import ng.appserver.templating.NGComponent;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.associations.NGConstantValueAssociation;

public class TestNGFileUpload {

	@Test
	public void rendersAFileInput() {
		assertEquals( "<input type=\"file\" name=\"upload\"/>", render() );
	}

	@Test
	public void passesOtherBindingsThroughAsAttributes() {
		assertEquals( "<input type=\"file\" name=\"upload\" accept=\"image/*\" class=\"wide\"/>", render( "accept", "image/*", "class", "wide" ) );
	}

	/**
	 * @return The rendered element named 'upload', with the given bindings as constant values, in order
	 */
	private static String render( final String... namesAndValues ) {
		final Map<String, NGAssociation> associations = new LinkedHashMap<>();
		associations.put( "name", new NGConstantValueAssociation( "upload" ) );
		associations.put( "data", new NGConstantValueAssociation( "not rendered" ) );

		for( int i = 0; i < namesAndValues.length; i += 2 ) {
			associations.put( namesAndValues[i], new NGConstantValueAssociation( namesAndValues[i + 1] ) );
		}

		final NGFileUpload element = new NGFileUpload( null, associations, null );

		final NGContext context = new TestContext( new TestRequest( "/no/1.0", Map.of() ), new NGSession() );
		context.setComponent( new TestPage( context, "" ) );

		final NGStandardResponse response = (NGStandardResponse)NGResponses.of();
		element.appendToResponse( response, context );
		return response.contentString();
	}

	/**
	 * An association that keeps the value it's set to
	 */
	private static class Receiver extends NGAssociation {

		Object value;

		@Override
		public void setValue( final Object value, final NGComponent component ) {
			this.value = value;
		}
	}

	private static NGMultipartRequest multipartRequest( final Part... parts ) {
		final NGMultipartParser parser = ( body, boundary ) -> List.of( parts ).iterator();
		return (NGMultipartRequest)NGRequests.of( "POST", "/no/1.0", null, "HTTP/1.1", Map.of( "Content-Type", List.of( "multipart/form-data; boundary=x" ) ), InputStream.nullInputStream(), null, parser );
	}

	private static Part file( final String name, final String filename, final String content ) {
		final byte[] bytes = content.getBytes( StandardCharsets.UTF_8 );
		return new Part( name, filename, Map.of( "Content-Type", List.of( "text/plain" ) ), new ByteArrayInputStream( bytes ), (long)bytes.length );
	}

	/**
	 * Takes values from the given request into an upload field named 'upload', whose filename and data are bound to the given receivers
	 */
	private static void takeValues( final NGRequest request, final Receiver filename, final Receiver data ) {
		takeValues( request, filename, "data", data );
	}

	/**
	 * Takes values from the given request into an upload field named 'upload', whose filename is bound to the given receiver, and the given content binding to the other
	 */
	private static void takeValues( final NGRequest request, final Receiver filename, final String contentBinding, final Receiver content ) {
		final Map<String, NGAssociation> associations = new LinkedHashMap<>();
		associations.put( "name", new NGConstantValueAssociation( "upload" ) );
		associations.put( "filename", filename );
		associations.put( contentBinding, content );

		final NGFileUpload element = new NGFileUpload( null, associations, null );

		final NGContext context = new TestContext( request, new NGSession() );
		context.setComponent( new TestPage( context, "" ) );
		element.takeValuesFromRequest( request, context );
	}

	@Test
	public void takesTheUploadedFile() {
		final Receiver filename = new Receiver();
		final Receiver data = new Receiver();
		takeValues( multipartRequest( file( "upload", "notes.txt", "contents" ) ), filename, data );
		assertEquals( "notes.txt", filename.value );
		assertArrayEquals( "contents".getBytes( StandardCharsets.UTF_8 ), (byte[])data.value );
	}

	@Test
	public void leavesTheBindingsAloneWithoutAFile() {
		final Receiver filename = new Receiver();
		final Receiver data = new Receiver();
		takeValues( multipartRequest( file( "other", "notes.txt", "contents" ) ), filename, data );
		assertNull( filename.value );
		assertNull( data.value );
	}

	@Test
	public void failsOnManyFiles() {
		assertThrows( IllegalStateException.class, () -> takeValues( multipartRequest( file( "upload", "a.txt", "a" ), file( "upload", "b.txt", "b" ) ), new Receiver(), new Receiver() ) );
	}

	@Test
	public void explainsAFormThatIsntMultipart() {
		final TestRequest request = new TestRequest( "/no/1.0", Map.of( "upload", List.of( "notes.txt" ) ) );
		final IllegalStateException e = assertThrows( IllegalStateException.class, () -> takeValues( request, new Receiver(), new Receiver() ) );
		assertTrue( e.getMessage().contains( "multipart/form-data" ) );
	}

	@Test
	public void handsOverAStream() throws IOException {
		final Receiver filename = new Receiver();
		final Receiver stream = new Receiver();
		takeValues( multipartRequest( file( "upload", "notes.txt", "contents" ) ), filename, "inputStream", stream );
		assertEquals( "notes.txt", filename.value );
		assertEquals( "contents", new String( ((InputStream)stream.value).readAllBytes(), StandardCharsets.UTF_8 ) );
	}

	@Test
	public void dataAndInputStreamCantBothBeBound() {
		final Map<String, NGAssociation> associations = new LinkedHashMap<>();
		associations.put( "data", new Receiver() );
		associations.put( "inputStream", new Receiver() );
		assertThrows( IllegalArgumentException.class, () -> new NGFileUpload( null, associations, null ) );
	}
}
