package ng.appserver.wointegration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
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

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.version( HttpClient.Version.HTTP_1_1 )
			.connectTimeout( Duration.ofSeconds( 5 ) )
			.build();

	private final ScheduledExecutorService _scheduler;
	private final String _appName;
	private final int _appPort;
	private final String _hostName;
	private final int _wotaskdPort;
	private final Duration _lifebeatInterval;

	public NGLifebeatThread( final String appName, final int appPort, final String hostName, final int wotaskdPort, final Duration lifebeatInterval ) {
		logger.info( "Creating LifebeatThread: appName={}, appPort={}, hostName={}, wotaskdPort={}, interval={}", appName, appPort, hostName, wotaskdPort, lifebeatInterval );

		Objects.requireNonNull( appName, "appName" );
		Objects.requireNonNull( hostName, "hostName" );
		Objects.requireNonNull( lifebeatInterval, "lifebeatInterval" );

		if( appPort < 1 ) {
			throw new IllegalArgumentException( "appPort must be a positive number" );
		}

		if( wotaskdPort < 1 ) {
			throw new IllegalArgumentException( "wotaskdPort must be a positive number" );
		}

		if( lifebeatInterval.isZero() || lifebeatInterval.isNegative() ) {
			throw new IllegalArgumentException( "lifebeatInterval must be positive" );
		}

		_appName = appName;
		_appPort = appPort;
		_hostName = hostName;
		_wotaskdPort = wotaskdPort;
		_lifebeatInterval = lifebeatInterval;

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
				_lifebeatInterval.toMillis(),
				_lifebeatInterval.toMillis(),
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

	private void sendWillCrash() {
		sendMessage( "willCrash" );
	}

	private void sendMessage( final String action ) {
		final String url = "http://%s:%d/cgi-bin/WebObjects/wotaskd.woa/wlb?%s&%s&%s&%d"
				.formatted( _hostName, _wotaskdPort, action, _appName, _hostName, _appPort );

		final HttpRequest request = HttpRequest.newBuilder()
				.uri( URI.create( url ) )
				.timeout( Duration.ofSeconds( 5 ) )
				.GET()
				.build();

		try {
			final HttpResponse<Void> response = CLIENT.send( request, BodyHandlers.discarding() );
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
