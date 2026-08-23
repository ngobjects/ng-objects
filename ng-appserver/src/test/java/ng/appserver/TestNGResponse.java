package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ng.appserver.http.NGRespBuilder;

public class TestNGResponse {

	@Test
	public void defaultResponseStatusIs200() {
		assertEquals( 200, NGRespBuilder.of().status() );
	}

	@Test
	public void status() {
		assertEquals( 400, NGRespBuilder.of( "Error", 400 ).status() );
	}
}