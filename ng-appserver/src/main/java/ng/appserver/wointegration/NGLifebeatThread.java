package ng.appserver.wointegration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends regular "lifebeats" to wotaskd to report application liveness.
 */

public class NGLifebeatThread {

	private static final Logger logger = LoggerFactory.getLogger( NGLifebeatThread.class );

	private final HttpClient _httpClient;
	private final ScheduledExecutorService _scheduler;
	private final String _baseUrl;
	private final long _lifebeatIntervalMS;

	public NGLifebeatThread( final String appName, final int appPort, final String wotaskdHost, final int wotaskdPort, final long lifebeatIntervalMS ) {
		logger.info( "Creating LifebeatThread: appName={}, appPort={}, wotaskdHost={}, wotaskdPort={}, intervalMS={}", appName, appPort, wotaskdHost, wotaskdPort, lifebeatIntervalMS );

		Objects.requireNonNull( appName, "appName" );
		Objects.requireNonNull( wotaskdHost, "wotaskdHost" );

		if( appPort < 1 ) {
			throw new IllegalArgumentException( "appPort must be a positive number" );
		}

		if( wotaskdPort < 1 ) {
			throw new IllegalArgumentException( "wotaskdPort must be a positive number" );
		}

		if( lifebeatIntervalMS < 1 ) {
			throw new IllegalArgumentException( "lifebeatIntervalMS must be a positive number" );
		}

		_lifebeatIntervalMS = lifebeatIntervalMS;

		// Base URL: http://wotaskdHost:wotaskdPort/cgi-bin/WebObjects/wotaskd.woa/wlb?{action}&{appName}&{wotaskdHost}&{appPort}
		_baseUrl = "http://" + wotaskdHost + ":" + wotaskdPort + "/cgi-bin/WebObjects/wotaskd.woa/wlb?%s&" + appName + "&" + wotaskdHost + "&" + appPort;

		_httpClient = HttpClient.newBuilder()
				.connectTimeout( Duration.ofSeconds( 5 ) )
				.build();

		_scheduler = Executors.newSingleThreadScheduledExecutor( r -> {
			final Thread t = new Thread( r, "LifebeatThread" );
			t.setDaemon( true );
			return t;
		} );
	}

	public void start() {
		sendHasStarted();

		_scheduler.scheduleAtFixedRate(
				() -> sendLifebeat(),
				_lifebeatIntervalMS,
				_lifebeatIntervalMS,
				TimeUnit.MILLISECONDS );
	}

	private void sendHasStarted() {
		sendMessage( "hasStarted" );
	}

	private void sendLifebeat() {
		sendMessage( "lifebeat" );
	}

	public void sendWillStop() {
		sendMessage( "willStop" );
	}

	/**
	 * FIXME: Note, we might be entering a loop here if wotaskd returns status 500 again for the willCrash message(s). Check wotaskd's behaviour (or use a different method for sending the message) // Hugi 2026-02-03
	 */
	private void sendWillCrash() {
		sendMessage( "willCrash" );
	}

	private void sendMessage( final String action ) {
		final String url = String.format( _baseUrl, action );

		try {
			final HttpRequest request = HttpRequest.newBuilder()
					.uri( URI.create( url ) )
					.timeout( Duration.ofSeconds( 5 ) )
					.GET()
					.build();

			final HttpResponse<Void> response = _httpClient.send( request, HttpResponse.BodyHandlers.discarding() );
			final int status = response.statusCode();

			if( status == 200 ) {
				logger.debug( "Lifebeat '{}' acknowledged", action );
			}
			else if( status == 500 ) {
				logger.info( "Force quit received from wotaskd. Exiting." );
				sendWillCrash();
				System.exit( 1 );
			}
			else {
				logger.debug( "Lifebeat '{}' returned status {}", action, status );
			}
		}
		catch( final Exception e ) {
			logger.debug( "Failed to send lifebeat '{}': {}", action, e.getMessage() );
		}
	}
}
