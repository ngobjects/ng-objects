package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestNGResponse {

	@Test
	public void defaultResponseStatusIs200() {
		assertEquals( 200, new NGResponse().status() );
	}

	@Test
	public void status() {
		assertEquals( 400, new NGResponse( "Error", 400 ).status() );
	}
}