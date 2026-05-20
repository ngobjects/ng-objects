package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TestNGMessage {

	@Test
	public void headersDictionaryIsCaseInsensitive() {
		// setHeader() method
		NGResponse r1 = NGRespBuilder.of();
		r1.setHeader( "some-header", "some-value" );
		assertEquals( List.of( "some-value" ), r1.headers().get( "SoMe-HeADeR" ) );

		// appendHeader() method
		NGResponse r2 = NGRespBuilder.of();
		r2.appendHeader( "some-header", "some-value" );
		assertEquals( List.of( "some-value" ), r2.headers().get( "SoMe-HeADeR" ) );

		// setHeaders() method
		NGResponse r3 = NGRespBuilder.of();
		r3.setHeaders( Map.of( "some-header", List.of( "some-value" ) ) );
		assertEquals( List.of( "some-value" ), r3.headers().get( "SoMe-HeADeR" ) );
	}

	@Test
	public void setHeaderReplacesExistingHeaderValues() {
		NGResponse r = NGRespBuilder.of();
		r.setHeader( "some-header", "some-value" );
		r.setHeader( "some-header", "some-other-value" );
		assertEquals( List.of( "some-other-value" ), r.headers().get( "some-header" ) );
	}

	@Test
	public void appendHeaderKeepsExistingHeaderValues() {
		NGResponse r = NGRespBuilder.of();
		r.appendHeader( "some-header", "some-value" );
		r.appendHeader( "some-header", "some-other-value" );
		assertEquals( List.of( "some-value", "some-other-value" ), r.headers().get( "some-header" ) );
	}

	@Test
	public void appendContentString() {
		NGResponse r = NGRespBuilder.of();
		r.appendContentString( "SomeText" );
		r.appendContentString( "MoreText" );
		assertEquals( "SomeTextMoreText", r.contentString() );
	}

	@Test
	public void contentStringSmokeTest() {
		NGResponse r = NGRespBuilder.of();
		r.setContentString( "Þjóðarþýðingin (icelandic stuff)" );
		assertEquals( "Þjóðarþýðingin (icelandic stuff)", r.contentString() );
	}

	@Test
	public void contentBytesSmokeTest() {
		NGResponse r = NGRespBuilder.of();
		byte[] initialBytes = "Hvað er að frétta".getBytes( StandardCharsets.UTF_8 );
		r.setContentBytes( initialBytes );
		assertArrayEquals( initialBytes, r.contentBytes() );
	}
}