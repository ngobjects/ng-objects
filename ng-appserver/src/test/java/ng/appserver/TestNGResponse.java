package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
		assertEquals( 400, NGResponses.of( "Error", 400 ).status() );
		assertEquals( "Error", NGResponses.of( "Error", 400 ).contentString() );
		assertEquals( 404, NGResponses.of( new byte[] { 1 }, 404 ).status() );
	}
}