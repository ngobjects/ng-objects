package ng.appserver.privates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import ng.appserver.NGResponse;

public class TestNGMessage {

	@Test
	public void testHeadersDictionaryIsCaseInsensitive() {
		NGResponse r = new NGResponse();
		r.setHeader( "some-header", "some-value" );

		assertEquals( List.of( "some-value" ), r.headers().get( "Some-Header" ) );
	}
}