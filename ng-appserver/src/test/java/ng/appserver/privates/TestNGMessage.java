package ng.appserver.privates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.NGResponse;

public class TestNGMessage {

	@Test
	public void headersDictionaryIsCaseInsensitive() {
		// setHeader() method
		NGResponse r1 = new NGResponse();
		r1.setHeader( "some-header", "some-value" );
		assertEquals( List.of( "some-value" ), r1.headers().get( "SoMe-HeADeR" ) );

		// appendHeader() method
		NGResponse r2 = new NGResponse();
		r2.appendHeader( "some-header", "some-value" );
		assertEquals( List.of( "some-value" ), r2.headers().get( "SoMe-HeADeR" ) );

		// setHeaders() method
		NGResponse r3 = new NGResponse();
		r3.setHeaders( Map.of( "some-header", List.of( "some-value" ) ) );
		assertEquals( List.of( "some-value" ), r3.headers().get( "SoMe-HeADeR" ) );
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