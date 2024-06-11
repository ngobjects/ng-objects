package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ng.appserver.privates.NGMimeTypes;

public class TestNGMimeTypes {

	@Test
	public void testMimeTypeForResourceName() {
		assertEquals( "image/jpeg", NGMimeTypes.mimeTypeForResourceName( "image.jpg" ) );
		assertEquals( "text/css", NGMimeTypes.mimeTypeForResourceName( "somefile.css" ) );
	}
}