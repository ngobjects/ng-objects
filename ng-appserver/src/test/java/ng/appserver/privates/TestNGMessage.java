package ng.appserver.privates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import ng.appserver.NGResponse;

public class TestNGMessage {

	@Test
	public void headersDictionaryIsCaseInsensitive() {
		NGResponse r = new NGResponse();
		r.setHeader( "some-header", "some-value" );
		assertEquals( List.of( "some-value" ), r.headers().get( "SoMe-HeADeR" ) );
	}

	@Test
	public void setHeaderReplacesExistingHeaderValues() {
		NGResponse r = new NGResponse();
		r.setHeader( "some-header", "some-value" );
		r.setHeader( "some-header", "some-other-value" );
		assertEquals( List.of( "some-other-value" ), r.headers().get( "some-header" ) );
	}

	@Test
	public void appendHeaderKeepsExistingHeaderValues() {
		NGResponse r = new NGResponse();
		r.appendHeader( "some-header", "some-value" );
		r.appendHeader( "some-header", "some-other-value" );
		assertEquals( List.of( "some-value", "some-other-value" ), r.headers().get( "some-header" ) );
	}
}