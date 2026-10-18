package ng.appserver.wointegration;

import java.time.Duration;

import ng.appserver.properties.NGProperties;

/**
 * Created as a temporary holder class for the LifeBeatThread, just to get it out of NGApplication // Hugi 2022-04-18
 */

public class NGLifebeatThreadRunner {

	private static NGLifebeatThread _lifebeatThread;

	/**
	 * Starts a lifebeat thread for communicating with wotaskd.
	 */
	public static void start( final NGProperties properties ) {
		final String hostName = properties.d().propWOHost();
		final String appName = properties.d().propWOApplicationName();
		final Integer appPort = properties.d().propWOPort();
		final Integer lifeBeatDestinationPort = properties.d().propWOLifebeatDestinationPort();
		final Duration lifebeatInterval = Duration.ofSeconds( properties.d().propWOLifebeatIntervalInSeconds() );

		_lifebeatThread = new NGLifebeatThread( appName, appPort, hostName, lifeBeatDestinationPort, lifebeatInterval );
		_lifebeatThread.start();
	}

	static NGLifebeatThread lifebeatThread() {
		return _lifebeatThread;
	}
}