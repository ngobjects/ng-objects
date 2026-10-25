package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGResponse;
import ng.appserver.http.NGResponses;

public class TestNGResponse {

	@Test
	public void defaultResponseStatusIs200() {
		assertEquals( 200, NGResponses.of().status() );
	}

	@Test
	public void status() {
		assertEquals( 400, NGResponses.of( 400, "Error" ).status() );
	}

	@Test
	public void statusFirstFactories() {
		assertEquals( "Error", NGResponses.of( 400, "Error" ).contentString() );
		assertEquals( 404, NGResponses.of( 404, new byte[] { 1, 2, 3 } ).status() );
		assertEquals( 3, NGResponses.of( 404, new byte[] { 1, 2, 3 } ).contentBytes().length );
	}

	@Test
	public void okFactories() {
		assertEquals( 200, NGResponses.ok( "Fine" ).status() );
		assertEquals( "Fine", NGResponses.ok( "Fine" ).contentString() );
		assertEquals( 200, NGResponses.ok( new byte[] { 1 } ).status() );
	}

	/**
	 * The old content-first overloads are deprecated but must keep working (and mean the same thing) until they're removed
	 */
	@Test
	@SuppressWarnings("deprecation")
	public void deprecatedContentFirstOverloadsStillWork() {
		assertEquals( 400, NGResponses.of( 400, "Error" ).status() );
		assertEquals( "Error", NGResponses.of( 400, "Error" ).contentString() );
		assertEquals( 404, NGResponses.of( 404, new byte[] { 1 } ).status() );
	}


	@Test
	public void headersDictionaryIsCaseInsensitive() {
		// setHeader() method
		NGResponse r1 = NGResponses.of();
		r1.setHeader( "some-header", "some-value" );
		assertEquals( List.of( "some-value" ), r1.headers().get( "SoMe-HeADeR" ) );

		// appendHeader() method
		NGResponse r2 = NGResponses.of();
		r2.appendHeader( "some-header", "some-value" );
		assertEquals( List.of( "some-value" ), r2.headers().get( "SoMe-HeADeR" ) );

		// setHeaders() method
		NGResponse r3 = NGResponses.of();
		r3.setHeaders( Map.of( "some-header", List.of( "some-value" ) ) );
		assertEquals( List.of( "some-value" ), r3.headers().get( "SoMe-HeADeR" ) );
	}

	@Test
	public void setHeaderReplacesExistingHeaderValues() {
		NGResponse r = NGResponses.of();
		r.setHeader( "some-header", "some-value" );
		r.setHeader( "some-header", "some-other-value" );
		assertEquals( List.of( "some-other-value" ), r.headers().get( "some-header" ) );
	}

	@Test
	public void appendHeaderKeepsExistingHeaderValues() {
		NGResponse r = NGResponses.of();
		r.appendHeader( "some-header", "some-value" );
		r.appendHeader( "some-header", "some-other-value" );
		assertEquals( List.of( "some-value", "some-other-value" ), r.headers().get( "some-header" ) );
	}

	@Test
	public void appendContentString() {
		NGResponse r = NGResponses.of();
		r.appendContentString( "SomeText" );
		r.appendContentString( "MoreText" );
		assertEquals( "SomeTextMoreText", r.contentString() );
	}

	@Test
	public void contentStringSmokeTest() {
		NGResponse r = NGResponses.of();
		r.setContentString( "Þjóðarþýðingin (icelandic stuff)" );
		assertEquals( "Þjóðarþýðingin (icelandic stuff)", r.contentString() );
	}

	@Test
	public void contentBytesSmokeTest() {
		NGResponse r = NGResponses.of();
		byte[] initialBytes = "Hvað er að frétta".getBytes( StandardCharsets.UTF_8 );
		r.setContentBytes( initialBytes );
		assertArrayEquals( initialBytes, r.contentBytes() );
	}
}
