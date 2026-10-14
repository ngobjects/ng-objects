package ng.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import ng.dev.NGDevInstanceStopper.Holder;

/**
 * Recognising what holds the development port, and reading the process that listens on it: only ng-objects and
 * WebObjects applications are ever stopped.
 */
public class TestNGDevInstanceStopper {

	@Test
	public void aWebObjectsApplication() {
		assertEquals( Holder.WO, NGDevInstanceStopper.holderFromCommandLine( "/jbr/bin/java -XX:HotswapAgent=fatjar -classpath /m2/com/webobjects/JavaWebObjects.jar:/app/classes app.Application -WOPort 1200" ) );
		assertEquals( Holder.WO, NGDevInstanceStopper.holderFromCommandLine( "/usr/bin/java -cp /m2/is/rebbi/wonder/ERExtensions/8.0.17/ERExtensions-8.0.17.jar app.Application" ) );
	}

	@Test
	public void anNGApplication() {
		assertEquals( Holder.NG, NGDevInstanceStopper.holderFromCommandLine( "/jbr/bin/java -cp /m2/is/rebbi/ng/ng-appserver/0.1.5/ng-appserver-0.1.5.jar:/app/classes app.Application" ) );
	}

	@Test
	public void anythingElseIsLeftAlone() {
		assertEquals( Holder.UNKNOWN, NGDevInstanceStopper.holderFromCommandLine( "/usr/bin/python3 -m http.server 1200" ) );
		assertEquals( Holder.UNKNOWN, NGDevInstanceStopper.holderFromCommandLine( "/jbr/bin/java -jar some-other-server.jar" ) );
		assertEquals( Holder.UNKNOWN, NGDevInstanceStopper.holderFromCommandLine( "node webobjects-mock.js" ) );
		assertEquals( Holder.UNKNOWN, NGDevInstanceStopper.holderFromCommandLine( null ) );
	}

	@Test
	public void resultsDescribeThemselves() {
		assertEquals( "Stopped the WO application on port 1200 (pid 7), killed after it ignored the request to stop", new NGDevInstanceStopper.Result( Holder.WO, 7L, true, true ).describe( 1200 ) );
		assertEquals( "Stopped the NG application on port 1201 (pid 8)", new NGDevInstanceStopper.Result( Holder.NG, 8L, false, true ).describe( 1201 ) );
		assertEquals( "Port 1200 is held by a process that isn't an ng-objects or WebObjects application; not stopping it", new NGDevInstanceStopper.Result( Holder.UNKNOWN, null, false, false ).describe( 1200 ) );
	}

	@Test
	public void lsofProcessIDs() {
		assertEquals( List.of( 95738L ), NGDevInstanceStopper.pids( "95738\n" ) );
		assertEquals( List.of( 1L, 2L ), NGDevInstanceStopper.pids( "1\nlsof: WARNING: can't stat() fuse\n2\n" ) );
		assertEquals( List.of(), NGDevInstanceStopper.pids( "" ) );
	}
}
