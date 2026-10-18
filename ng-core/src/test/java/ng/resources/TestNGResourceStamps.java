package ng.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class TestNGResourceStamps {

	private static final String STAMP = "3f9c1e07ab";

	@Test
	public void theStampIsTenHexDigitsOfTheContent() {
		final String stamp = NGResourceStamps.stamp( "body {}".getBytes( StandardCharsets.UTF_8 ) );
		assertEquals( 10, stamp.length() );
		assertEquals( stamp, NGResourceStamps.stamp( "body {}".getBytes( StandardCharsets.UTF_8 ) ) );
		assertNotEquals( stamp, NGResourceStamps.stamp( "body { }".getBytes( StandardCharsets.UTF_8 ) ) );
	}

	@Test
	public void aStreamIsStampedAsItsContentIs() throws IOException {
		final byte[] content = new byte[1_000_000]; // larger than a stream transfer's buffer, so digested in chunks
		new Random( 1 ).nextBytes( content );
		assertEquals( NGResourceStamps.stamp( content ), NGResourceStamps.stamp( new ByteArrayInputStream( content ) ) );
	}

	@Test
	public void theStampGoesBeforeTheExtensionMarkedByAnAt() {
		assertEquals( "site@3f9c1e07ab.css", NGResourceStamps.stampedPath( "site.css", STAMP ) );
		assertEquals( "css/site@3f9c1e07ab.css", NGResourceStamps.stampedPath( "css/site.css", STAMP ) );
		assertEquals( "archive.tar@3f9c1e07ab.gz", NGResourceStamps.stampedPath( "archive.tar.gz", STAMP ) );
		assertEquals( "LICENSE@3f9c1e07ab", NGResourceStamps.stampedPath( "LICENSE", STAMP ) );
		assertEquals( "x/.htaccess@3f9c1e07ab", NGResourceStamps.stampedPath( "x/.htaccess", STAMP ) );
		assertEquals( "img/logo@2x@3f9c1e07ab.png", NGResourceStamps.stampedPath( "img/logo@2x.png", STAMP ) );
	}

	@Test
	public void aStampedPathParsesBackToThePathAndTheStamp() {
		for( final String path : new String[] { "site.css", "app/css/site.css", "archive.tar.gz", "LICENSE", "x/.htaccess", "my.file.name.js", "app/img/logo@2x.png" } ) {
			final NGResourceStamps.Stamped stamped = NGResourceStamps.parse( NGResourceStamps.stampedPath( path, STAMP ) );
			assertEquals( path, stamped.unstampedPath() );
			assertEquals( STAMP, stamped.stamp() );
		}
	}

	@Test
	public void anUnstampedPathHasNoStamp() {
		assertNull( NGResourceStamps.parse( "app/css/site.css" ) );
		assertNull( NGResourceStamps.parse( "AjaxSlim/ajaxslim.js" ) );
		assertNull( NGResourceStamps.parse( "app/x.3F9C1E07AB.css" ) ); // uppercase isn't a stamp
		assertNull( NGResourceStamps.parse( "app/x.3f9c1e07a.css" ) ); // nine digits isn't one
		assertNull( NGResourceStamps.parse( "app/3f9c1e07ab.css" ) ); // nothing before it: the name itself
		assertNull( NGResourceStamps.parse( "app/x.3f9c1e07ab/y.css" ) ); // in a folder name, not the file's
		assertNull( NGResourceStamps.parse( "app/x@3f9c1e07ab/y.css" ) ); // nor with the marker
		assertNull( NGResourceStamps.parse( "app/img/logo@2x.png" ) ); // a density variant, not a stamp
		assertNull( NGResourceStamps.parse( "app/x@3F9C1E07AB.css" ) ); // uppercase isn't a stamp
	}

	@Test
	public void theMarkerIsAcceptedEncoded() {
		final NGResourceStamps.Stamped stamped = NGResourceStamps.parse( "app/css/site%403f9c1e07ab.css" );
		assertEquals( "app/css/site.css", stamped.unstampedPath() );
		assertEquals( STAMP, stamped.stamp() );
	}

	@Test
	public void theFormerPeriodFormIsAName() {
		// A file whose own name is a period and ten hex digits is that file, not a stamped one
		assertNull( NGResourceStamps.parse( "app/css/site.3f9c1e07ab.css" ) );
		assertNull( NGResourceStamps.parse( "app/js/chart.0123456789.js" ) );
	}

	@Test
	public void aDensityVariantIsStampedAndParsedBack() {
		final NGResourceStamps.Stamped stamped = NGResourceStamps.parse( "app/img/logo@2x@3f9c1e07ab.png" );
		assertEquals( "app/img/logo@2x.png", stamped.unstampedPath() );
		assertEquals( STAMP, stamped.stamp() );
	}
}
