package ng.appserver.wointegration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

	private final ScheduledExecutorService _scheduler;
	private final String _wotaskdHost;
	private final int _wotaskdPort;
	private final String _requestTemplate;
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

		_wotaskdHost = wotaskdHost;
		_wotaskdPort = wotaskdPort;
		_lifebeatIntervalMS = lifebeatIntervalMS;

		// Request template: GET /path HTTP/1.0\r\nHost: host:port\r\n\r\n
		// Using HTTP/1.0 to signal wotaskd to close the connection after responding
		_requestTemplate = "GET /cgi-bin/WebObjects/wotaskd.woa/wlb?%s&" + appName + "&" + wotaskdHost + "&" + appPort + " HTTP/1.0\r\nHost: " + wotaskdHost + ":" + wotaskdPort + "\r\n\r\n";

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

	private void sendWillCrash() {
		sendMessage( "willCrash" );
	}

	/**
	 * Sends a message to wotaskd and reads just the HTTP status line.
	 * Uses raw sockets because wotaskd doesn't send proper HTTP response termination (no Content-Length, no Connection: close for HTTP/1.1).
	 */
	private void sendMessage( final String action ) {
		final String request = String.format( _requestTemplate, action );

		try( final Socket socket = new Socket( _wotaskdHost, _wotaskdPort )) {
			socket.setSoTimeout( 5000 );

			// Send request
			final OutputStream out = socket.getOutputStream();
			out.write( request.getBytes( StandardCharsets.UTF_8 ) );
			out.flush();

			// Read just the status line (e.g., "HTTP/1.0 200 Apple WebObjects")
			final BufferedReader reader = new BufferedReader( new InputStreamReader( socket.getInputStream(), StandardCharsets.UTF_8 ) );
			final String statusLine = reader.readLine();

			if( statusLine == null ) {
				logger.info( "No response from wotaskd for '{}'", action );
				return;
			}

			// Parse status code from "HTTP/1.x NNN ..."
			final int status = parseStatusCode( statusLine );

			if( status == 200 ) {
				logger.info( "Lifebeat '{}' acknowledged", action );
			}
			else if( status == 500 ) {
				logger.info( "Force quit received from wotaskd. Exiting." );
				sendWillCrash();
				System.exit( 1 );
			}
			else {
				logger.info( "Lifebeat '{}' returned status {}", action, status );
			}
		}
		catch( final Exception e ) {
			logger.info( "Failed to send lifebeat '{}': {}", action, e.getMessage() );
		}
	}

	/**
	 * Extracts the HTTP status code from a status line like "HTTP/1.0 200 OK"
	 */
	private static int parseStatusCode( final String statusLine ) {
		final String[] parts = statusLine.split( " " );

		if( parts.length >= 2 ) {
			try {
				return Integer.parseInt( parts[1] );
			}
			catch( final NumberFormatException e ) {
				return -1;
			}
		}

		return -1;
	}
}
