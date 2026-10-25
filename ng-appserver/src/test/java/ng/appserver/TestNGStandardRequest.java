package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
		return request( null, null, contentStream );
	}

	private static NGStandardRequest request( final String queryString, final String contentType, final InputStream contentStream ) {
		final Map<String, List<String>> headers = contentType == null ? Map.of() : Map.of( "Content-Type", List.of( contentType ) );
		return new NGStandardRequest( "POST", "/", queryString, "HTTP/1.1", headers, contentStream, null );
	}

	private static final String FORM = "application/x-www-form-urlencoded";

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

	@Test
	public void formValuesFromTheQueryString() {
		final NGStandardRequest request = request( "a=1&b=x+y", null, InputStream.nullInputStream() );
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "x y" ) ), request.formValues() );
		assertEquals( "1", request.formValueForKey( "a" ) );
	}

	@Test
	public void noQueryStringAndNoFormBodyMeansNoFormValues() {
		assertEquals( Map.of(), request( InputStream.nullInputStream() ).formValues() );
	}

	@Test
	public void formValuesFromAFormEncodedBody() {
		final NGStandardRequest request = request( null, FORM, new RecordingStream( "a=1&b=2" ) );
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "2" ) ), request.formValues() );
	}

	@Test
	public void queryStringValuesComeBeforeTheBodys() {
		final NGStandardRequest request = request( "a=query&q=1", FORM, new RecordingStream( "a=body&b=2" ) );
		assertEquals( List.of( "query", "body" ), request.formValuesForKey( "a" ) );
		assertEquals( List.of( "a", "q", "b" ), List.copyOf( request.formValues().keySet() ) );
	}

	@Test
	public void formContentTypeIsMatchedWithoutItsParametersOrCase() {
		assertEquals( "1", request( null, "Application/X-WWW-Form-URLEncoded ; charset=UTF-8", new RecordingStream( "a=1" ) ).formValueForKey( "a" ) );
	}

	@Test
	public void bodyCharset() {
		final InputStream body = new ByteArrayInputStream( "nafn=%DE%F3r%F0ur".getBytes( StandardCharsets.US_ASCII ) );
		assertEquals( "Þórður", request( null, FORM + "; charset=ISO-8859-1", body ).formValueForKey( "nafn" ) );
	}

	@Test
	public void unknownCharsetIsUTF8() {
		final InputStream body = new ByteArrayInputStream( "nafn=%C3%9E".getBytes( StandardCharsets.US_ASCII ) );
		assertEquals( "Þ", request( null, FORM + "; charset=no-such-charset", body ).formValueForKey( "nafn" ) );
	}

	@Test
	public void theRawBodySurvivesFormParsing() {
		final NGStandardRequest request = request( null, FORM, new RecordingStream( "a=1+1&b=%26" ) );
		assertEquals( "1 1", request.formValueForKey( "a" ) );
		assertEquals( "a=1+1&b=%26", request.contentString() );
	}

	@Test
	public void formValuesCanBeParsedAfterTheRawBodyIsRead() {
		final NGStandardRequest request = request( null, FORM, new RecordingStream( "a=1" ) );
		assertEquals( "a=1", request.contentString() );
		assertEquals( "1", request.formValueForKey( "a" ) );
	}

	@Test
	public void formValuesDontReadABodyThatIsntAForm() {
		final RecordingStream stream = new RecordingStream( "a=1" );
		final NGStandardRequest request = request( "q=1", "text/plain", stream );
		assertEquals( Map.of( "q", List.of( "1" ) ), request.formValues() );
		assertFalse( stream.read );
	}

	@Test
	public void toStringDoesntReadTheBody() {
		final RecordingStream stream = new RecordingStream( "a=1" );
		request( "q=1", FORM, stream ).toString();
		assertFalse( stream.read );
	}

	@Test
	public void formBodyLengthLimit() {
		final String body = "a=" + "x".repeat( NGStandardRequest.MAX_FORM_CONTENT_LENGTH );
		assertThrows( IllegalArgumentException.class, () -> request( null, FORM, new RecordingStream( body ) ).formValues() );
	}

	@Test
	public void theRawBodyHasNoFormLengthLimit() {
		final String body = "x".repeat( NGStandardRequest.MAX_FORM_CONTENT_LENGTH + 1 );
		assertEquals( body.length(), request( null, "text/plain", new RecordingStream( body ) ).contentBytes().length );
	}

	@Test
	public void formFieldLimit() {
		final String body = "a=1&".repeat( NGStandardRequest.MAX_FORM_FIELDS + 1 );
		assertThrows( IllegalArgumentException.class, () -> request( null, FORM, new RecordingStream( body ) ).formValues() );
	}

	private static NGStandardRequest requestWithHeaders( final Map<String, List<String>> headers ) {
		return new NGStandardRequest( "GET", "/", null, "HTTP/1.1", headers, InputStream.nullInputStream(), null );
	}

	@Test
	public void cookieValuesFromTheCookieHeaders() {
		final NGStandardRequest request = requestWithHeaders( Map.of( "Cookie", List.of( "a=1; b=2", "c=3" ) ) );
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "2" ), "c", List.of( "3" ) ), request.cookieValues() );
		assertEquals( "2", request.cookieValueForKey( "b" ) );
	}

	@Test
	public void noCookieHeaderMeansNoCookies() {
		final NGStandardRequest request = requestWithHeaders( Map.of() );
		assertEquals( Map.of(), request.cookieValues() );
		assertNull( request.cookieValueForKey( "a" ) );
	}

	@Test
	public void theSessionIDComesFromItsCookie() {
		final NGStandardRequest request = requestWithHeaders( Map.of( "cookie", List.of( "other=x; " + NGApplication.SESSION_ID_COOKIE_NAME + "=abc123" ) ) );
		assertEquals( "abc123", request._sessionID() );
	}

	@Test
	public void aRepeatedFormValueGivesTheFirst() {
		final NGStandardRequest request = request( "a=1&a=2", null, InputStream.nullInputStream() );
		assertEquals( "1", request.formValueForKey( "a" ) );
		assertEquals( List.of( "1", "2" ), request.formValuesForKey( "a" ) );
	}

	@Test
	public void aRepeatedCookieGivesTheFirst() {
		final NGStandardRequest request = requestWithHeaders( Map.of( "Cookie", List.of( "a=1; a=2" ) ) );
		assertEquals( "1", request.cookieValueForKey( "a" ) );
		assertEquals( List.of( "1", "2" ), request.cookieValuesForKey( "a" ) );
	}

	@Test
	public void aRepeatedHeaderGivesTheFirst() {
		final NGStandardRequest request = requestWithHeaders( Map.of( "Accept-Language", List.of( "is", "en" ) ) );
		assertEquals( "is", request.headerForKey( "accept-language" ) );
		assertEquals( List.of( "is", "en" ), request.headersForKey( "accept-language" ) );
	}

	@Test
	public void headersCantBeModified() {
		final NGStandardRequest request = requestWithHeaders( Map.of( "X-Thing", List.of( "1" ) ) );
		assertEquals( "1", request.headerForKey( "x-thing" ) );
		assertThrows( UnsupportedOperationException.class, () -> request.headers().put( "X-Other", List.of( "2" ) ) );
	}
}
