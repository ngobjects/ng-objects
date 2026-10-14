package ng.appserver.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.dev.NGDevInstanceStopper;

/**
 * If an application fails to bind to the port, stopPreviousDevelopmentInstance() stops the ng-objects or WebObjects
 * application holding it (see {@link NGDevInstanceStopper}), so the adaptor can retry binding. Tried once: if the port
 * still can't be had, the application terminates.
 */

public class NGDevelopmentInstanceStopper {

	private static final Logger logger = LoggerFactory.getLogger( NGDevelopmentInstanceStopper.class );

	/**
	 * Indicates if we already tried stopping an existing development instance (we only try that once)
	 */
	private static boolean alreadyTriedStopping = false;

	public static void stopPreviousDevelopmentInstance( int portNumber ) {
		if( alreadyTriedStopping ) {
			logger.info( "We've already unsuccessfully tried stopping a previous application instance, and it didn't work. No sense trying again. Exiting" );
			NGApplication.application().terminate();
		}

		alreadyTriedStopping = true;

		final NGDevInstanceStopper.Result result = NGDevInstanceStopper.stopInstanceOnPort( portNumber );

		if( result.released() ) {
			logger.info( result.describe( portNumber ) );
		}
		else {
			logger.warn( result.describe( portNumber ) );
		}
	}
}
