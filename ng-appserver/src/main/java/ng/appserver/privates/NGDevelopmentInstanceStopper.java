package ng.appserver.privates;

import java.io.InputStream;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;

/**
 * If an application fails to bind to the port, stopPreviousDevelopmentInstance() can be invoked to try to kill an existing running application instance.
 * The adaptor can then retry binding to the port.
 */

public class NGDevelopmentInstanceStopper {

	private static final Logger logger = LoggerFactory.getLogger( NGDevelopmentInstanceStopper.class );

	/**
	 * Indicates if we already tried stopping an existing development instance (we only try that once)
	 */
	private static boolean alreadyTriedStopping = false;

	/**
	 * Kill an existing development instance running on the given port (either an ng-objects app or a WO app)
	 */
	public static void stopPreviousDevelopmentInstance( int portNumber ) {
		if( alreadyTriedStopping ) {
			logger.info( "We've already unsuccessfully tried stopping a previous application instance, and it didn't work. No sense trying again. Exiting" );
			NGApplication.application().terminate();
		}

		try {
			alreadyTriedStopping = true;

			String url = "http://localhost:" + portNumber;

			if( isNGApplicationRunningInPort( portNumber ) ) {
				url += "/wa/NGAdminAction/terminate";
			}
			else {
				// If not an ng-objects application, try killing a WO instance
				url += "/Apps/WebObjects/SomeApp.woa/wa/ERXDirectAction/stop";
			}

			new URI( url ).toURL().openConnection().getContent();
			Thread.sleep( 2000 );
		}
		catch( Throwable e ) {
			logger.info( "Terminated existing development instance" );
		}
	}

	/**
	 * @return true if the application running on the given port number is an ng-objects application
	 */
	private static boolean isNGApplicationRunningInPort( int portNumber ) {
		final String urlString = String.format( "http://localhost:%s/wa/NGAdminAction/type", 1200 );

		try( InputStream is = new URI( urlString ).toURL().openStream()) {
			final String type = new String( is.readAllBytes() );
			return "ng".equals( type );
		}
		catch( Throwable e ) {
			return false;
		}
	}
}