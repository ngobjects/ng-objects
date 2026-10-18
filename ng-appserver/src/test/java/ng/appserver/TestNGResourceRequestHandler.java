package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGResponse;
import ng.appserver.resources.NGResource;

public class TestNGResourceRequestHandler {

	private static final String STAMP = "3f9c1e07ab";

	private static final NGResource RESOURCE = NGResource.of( () -> new ByteArrayInputStream( "body {}".getBytes( StandardCharsets.UTF_8 ) ) );

	@Test
	public void theCurrentStampIsCachedForGood() {
		final NGResponse response = NGResourceRequestHandler.responseForWebserverResource( RESOURCE, "css/site.css", STAMP, STAMP, null, true );
		assertEquals( 200, response.status() );
		assertEquals( "public, max-age=31536000, immutable", response.headerForKey( "cache-control" ) );
		assertEquals( "\"" + STAMP + "\"", response.headerForKey( "etag" ) );
		assertEquals( "body {}", response.contentString() );
	}

	@Test
	public void anUnstampedURLMustRevalidate() {
		final NGResponse response = NGResourceRequestHandler.responseForWebserverResource( RESOURCE, "css/site.css", null, STAMP, null, true );
		assertEquals( "no-cache", response.headerForKey( "cache-control" ) );
		assertEquals( "\"" + STAMP + "\"", response.headerForKey( "etag" ) );
	}

	@Test
	public void aStampThatIsntTheResourcesOwnMustRevalidate() {
		final NGResponse response = NGResourceRequestHandler.responseForWebserverResource( RESOURCE, "css/site.css", "0123456789", STAMP, null, true );
		assertEquals( "no-cache", response.headerForKey( "cache-control" ) );
	}

	@Test
	public void theCurrentStampMustRevalidateInDevelopment() {
		final NGResponse response = NGResourceRequestHandler.responseForWebserverResource( RESOURCE, "css/site.css", STAMP, STAMP, null, false );
		assertEquals( "no-cache", response.headerForKey( "cache-control" ) );
	}

	@Test
	public void theCurrentETagIsNotModified() {
		final NGResponse response = NGResourceRequestHandler.responseForWebserverResource( RESOURCE, "css/site.css", null, STAMP, "\"" + STAMP + "\"", true );
		assertEquals( 304, response.status() );
		assertEquals( "\"" + STAMP + "\"", response.headerForKey( "etag" ) );
		assertEquals( "no-cache", response.headerForKey( "cache-control" ) );
	}

	@Test
	public void aResourceWithoutAStampHasNoETag() {
		final NGResponse response = NGResourceRequestHandler.responseForWebserverResource( RESOURCE, "css/site.css", null, null, "*", true );
		assertEquals( 200, response.status() );
		assertNull( response.headerForKey( "etag" ) );
	}

	@Test
	public void ifNoneMatchIsComparedWeakly() {
		final String etag = "\"" + STAMP + "\"";
		assertTrue( NGResourceRequestHandler.matchesAny( etag, etag ) );
		assertTrue( NGResourceRequestHandler.matchesAny( "W/" + etag, etag ) );
		assertTrue( NGResourceRequestHandler.matchesAny( "\"0123456789\", " + etag, etag ) );
		assertTrue( NGResourceRequestHandler.matchesAny( "*", etag ) );
		assertFalse( NGResourceRequestHandler.matchesAny( "\"0123456789\"", etag ) );
		assertFalse( NGResourceRequestHandler.matchesAny( null, etag ) );
	}
}
