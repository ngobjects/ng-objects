package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ng.appserver.privates.NGMimeTypeDetector;

public class TestNGMimeTypeDetector {

	@Test
	public void testMimeTypeForResourceName() {
		assertEquals( "image/jpeg", NGMimeTypeDetector.mimeTypeForResourceName( "image.jpg" ) );
		assertEquals( "text/css", NGMimeTypeDetector.mimeTypeForResourceName( "somefile.css" ) );
	}
}