package ng.appserver.privates;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;

public class NGDevelopmentInstanceStopper {

	private static final Logger logger = LoggerFactory.getLogger( NGDevelopmentInstanceStopper.class );

	/**
	 * Indicates if we already tried stopping an existing development instance (we only try that once)
	 */
	private static boolean alreadyTriedStopping = false;

	/**
	 * FIXME: Kill an existing WO application if that's what's blocking the instance
	 */
	public static void stopPreviousDevelopmentInstance( int portNumber ) {
		if( alreadyTriedStopping ) {
			logger.info( "We've already unsuccessfully tried stopping a previous application instance, and it didn't work. No sense trying again. Exiting" );
			NGApplication.application().terminate();
		}

		try {
			final String urlString = String.format( "http://localhost:%s/wa/ng.appserver.privates.NGAdminAction/terminate", portNumber );
			new URL( urlString ).openConnection().getContent();
			Thread.sleep( 1000 );
			alreadyTriedStopping = true;
		}
		catch( Throwable e ) {
			logger.info( "Terminated existing development instance" );
		}
	}
}