package ng.appserver.privates;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;

public class _NGUtilities {

	private static final Logger logger = LoggerFactory.getLogger( _NGUtilities.class );

	/**
	 * @return true if the given object is "truthy" for conditionals
	 *
	 * The conditions under which that is are
	 * - boolean true
	 * - a number that's exactly zero
	 * - null
	 */
	public static boolean isTruthy( Object object ) {

		if( object == null ) {
			return false;
		}

		if( object instanceof Boolean b ) {
			return b;
		}

		if( object instanceof Number n ) {
			return n.doubleValue() != 0;
		}

		return true;
	}

	private static boolean alreadyTriedStopping = false;

	/**
	 * FIXME: This functionality should really be in a nicer location // Hugi 2021-11-20
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