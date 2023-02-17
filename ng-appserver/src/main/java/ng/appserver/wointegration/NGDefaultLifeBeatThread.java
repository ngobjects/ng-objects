package ng.appserver.wointegration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import ng.appserver.NGProperties;

/**
 * FIXME: Created as a temporary holder class for the LifeBeatThread, just to get it out of NGApplication // Hugi 2022-04-18
 */

public class NGDefaultLifeBeatThread {

	/**
	 * FIXME: public for the benefit of WOMPRequestHandler, which uses it to generate messages to send to wotaskd. Let's look into that // Hugi 2021-12-29
	 */
	public static NGLifebeatThread _lifebeatThread;

	/**
	 * Starts a lifebeat thread for communicating with wotaskd.
	 */
	public static void start( final NGProperties properties ) {
		final String hostName = properties.propWOHost();
		final String appName = properties.propWOApplicationName();
		final Integer appPort = properties.propWOPort();
		final Integer lifeBeatDestinationPort = properties.propWOLifebeatDestinationPort();
		final Integer lifeBeatIntervalInSeconds = properties.propWOLifebeatIntervalInSeconds();
		final long lifeBeatIntervalInMilliseconds = TimeUnit.MILLISECONDS.convert( lifeBeatIntervalInSeconds, TimeUnit.SECONDS );

		InetAddress hostAddress = null;

		try {
			hostAddress = InetAddress.getByName( hostName );
		}
		catch( final UnknownHostException e ) {
			throw new RuntimeException( "Failed to start LifebeatThread", e );
		}

		_lifebeatThread = new NGLifebeatThread( appName, appPort, hostAddress, lifeBeatDestinationPort, lifeBeatIntervalInMilliseconds );
		_lifebeatThread.setDaemon( true );
		_lifebeatThread.start();
	}
}