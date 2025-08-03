package ng.appserver.wointegration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import ng.appserver.properties.NGProperties;

/**
 * Created as a temporary holder class for the LifeBeatThread, just to get it out of NGApplication // Hugi 2022-04-18
 */

public class NGDefaultLifeBeatThread {

	private static NGLifebeatThread _lifebeatThread;

	/**
	 * Starts a lifebeat thread for communicating with wotaskd.
	 */
	public static void start( final NGProperties properties ) {
		final String hostName = properties.d().propWOHost();
		final String appName = properties.d().propWOApplicationName();
		final Integer appPort = properties.d().propWOPort();
		final Integer lifeBeatDestinationPort = properties.d().propWOLifebeatDestinationPort();
		final Integer lifeBeatIntervalInSeconds = properties.d().propWOLifebeatIntervalInSeconds();
		final long lifeBeatIntervalInMilliseconds = TimeUnit.MILLISECONDS.convert( lifeBeatIntervalInSeconds, TimeUnit.SECONDS );

		final InetAddress hostAddress;

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

	/**
	 * Public for the benefit of WOMPRequestHandler which will use it to generate messages to send to wotaskd
	 */
	public static NGLifebeatThread lifebeatThread() {
		return _lifebeatThread;
	}
}