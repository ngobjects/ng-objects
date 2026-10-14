package ng.dev;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Takes the development port from the application holding it, for an application that couldn't bind to it. Shared by both
 * frameworks so the rule is defined once and can't drift between them: each decides when to call it (development only,
 * once) and logs the {@link Result}.
 *
 * Any ng-objects or WebObjects application on the port is stopped, and firmly: first asked to stop (ng's
 * {@code /ng/dev/terminate}, Wonder's stop action for a WebObjects application), and if it still holds the port a few seconds
 * later — a JVM wedged after a failed hot swap ignores the request — its process is killed ({@code kill -9}). A process
 * that can't be identified as an ng or WO application is never touched.
 */
public final class NGDevInstanceStopper {

	/**
	 * Wonder's stop action. A development instance is connected to directly, and then answers it under any application
	 * name in the URL, so the holder's name doesn't need to be known. Plain WebObjects has no stop action: its process is killed.
	 */
	private static final String WO_STOP_PATH = "/Apps/WebObjects/App.woa/wa/ERXDirectAction/stop";

	/**
	 * How long a request to the running application may take. A hung application must not hang this one's startup.
	 */
	private static final int REQUEST_TIMEOUT_MILLIS = 2000;

	/**
	 * How long a stopped application gets to release the port, before (and again after) being killed
	 */
	private static final long RELEASE_TIMEOUT_MILLIS = 4000;

	/**
	 * What holds the port
	 */
	public enum Holder {
		NG,
		WO,
		UNKNOWN
	}

	/**
	 * What happened: what held the port, its process id (null if it couldn't be found), whether it had to be killed, and
	 * whether the port is free now
	 */
	public record Result( Holder holder, Long pid, boolean killed, boolean released ) {

		/**
		 * @return A sentence describing the result, for the caller's log
		 */
		public String describe( final int port ) {
			final String process = pid != null ? " (pid " + pid + ")" : "";

			if( holder == Holder.UNKNOWN ) {
				return "Port %d is held by a process that isn't an ng-objects or WebObjects application%s; not stopping it".formatted( port, process );
			}

			if( !released ) {
				return "Port %d is still held by the %s application%s%s".formatted( port, holder, process, killed ? ", even after killing it" : "; its process couldn't be found to kill it" );
			}

			return "Stopped the %s application on port %d%s%s".formatted( holder, port, process, killed ? ", killed after it ignored the request to stop" : "" );
		}
	}

	private NGDevInstanceStopper() {}

	/**
	 * Stops the ng-objects or WebObjects application on the port: asks it to stop, then kills its process if it doesn't
	 */
	public static Result stopInstanceOnPort( final int port ) {
		final Optional<ProcessHandle> process = listeningProcess( port );
		final Long pid = process.map( ProcessHandle::pid ).orElse( null );
		final Holder holder = holder( port, process );

		if( holder == Holder.UNKNOWN ) {
			return new Result( holder, pid, false, false );
		}

		request( port, holder == Holder.NG ? "/ng/dev/terminate" : WO_STOP_PATH );

		if( portReleasedWithin( port, RELEASE_TIMEOUT_MILLIS ) ) {
			return new Result( holder, pid, false, true );
		}

		if( process.isEmpty() ) {
			return new Result( holder, null, false, false );
		}

		process.get().destroyForcibly();
		return new Result( holder, pid, true, portReleasedWithin( port, RELEASE_TIMEOUT_MILLIS ) );
	}

	/**
	 * @return What holds the port: an ng-objects application answers {@code /ng/dev/type}; a WebObjects application is
	 * recognised by its process's command line
	 */
	private static Holder holder( final int portNumber, final Optional<ProcessHandle> process ) {
		if( isNGApplicationRunningInPort( portNumber ) ) {
			return Holder.NG;
		}

		final String commandLine = process.flatMap( p -> p.info().commandLine() ).orElse( null );
		return holderFromCommandLine( commandLine );
	}

	/**
	 * @return The holder a process's command line identifies: a Java process running WebObjects (or Wonder) is WO, one
	 * running ng-objects is NG, anything else (or an unknown command line) is UNKNOWN
	 */
	static Holder holderFromCommandLine( final String commandLine ) {
		if( commandLine == null ) {
			return Holder.UNKNOWN;
		}

		final String c = commandLine.toLowerCase( Locale.ROOT );

		if( !c.contains( "java" ) ) {
			return Holder.UNKNOWN;
		}

		if( c.contains( "ng-appserver" ) ) {
			return Holder.NG;
		}

		if( c.contains( "webobjects" ) || c.contains( "-woport" ) || c.contains( "erextensions" ) || c.contains( "wonder" ) ) {
			return Holder.WO;
		}

		return Holder.UNKNOWN;
	}

	/**
	 * @return true if the application running on the given port number is an ng-objects application
	 */
	private static boolean isNGApplicationRunningInPort( final int portNumber ) {
		return "ng".equals( request( portNumber, "/ng/dev/type" ) );
	}

	/**
	 * Requests the path from the application on the port, with timeouts
	 *
	 * @return The response body, or null if there's no answer
	 */
	private static String request( final int portNumber, final String path ) {
		try {
			final HttpURLConnection connection = (HttpURLConnection)new URI( "http://localhost:" + portNumber + path ).toURL().openConnection();
			connection.setConnectTimeout( REQUEST_TIMEOUT_MILLIS );
			connection.setReadTimeout( REQUEST_TIMEOUT_MILLIS );

			try( InputStream is = connection.getInputStream()) {
				return new String( is.readAllBytes(), StandardCharsets.UTF_8 );
			}
		}
		catch( Exception e ) {
			// No answer: the application isn't one, is stopping (a stop request often ends with the connection dropping), or hangs
			return null;
		}
	}

	/**
	 * @return The process listening on the port, found with {@code lsof}; empty when there's none or it can't be determined
	 */
	private static Optional<ProcessHandle> listeningProcess( final int portNumber ) {
		try {
			final Process lsof = new ProcessBuilder( "lsof", "-nP", "-t", "-iTCP:" + portNumber, "-sTCP:LISTEN" ).redirectErrorStream( true ).start();

			if( !lsof.waitFor( REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS ) ) {
				lsof.destroyForcibly();
				return Optional.empty();
			}

			final List<Long> pids = pids( new String( lsof.getInputStream().readAllBytes(), StandardCharsets.UTF_8 ) );
			final long self = ProcessHandle.current().pid();
			return pids.stream().filter( pid -> pid != self ).findFirst().flatMap( ProcessHandle::of );
		}
		catch( IOException e ) {
			return Optional.empty(); // lsof isn't available
		}
		catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	/**
	 * @return The process ids in {@code lsof -t} output (one per line)
	 */
	static List<Long> pids( final String lsofOutput ) {
		final List<Long> pids = new ArrayList<>();

		for( final String line : lsofOutput.split( "\\R" ) ) {
			try {
				pids.add( Long.parseLong( line.strip() ) );
			}
			catch( NumberFormatException e ) {
				// Not a pid (an lsof warning, or a blank line)
			}
		}

		return pids;
	}

	/**
	 * @return true if nothing listens on the port within the given time
	 */
	private static boolean portReleasedWithin( final int portNumber, final long millis ) {
		final long deadline = System.currentTimeMillis() + millis;

		while( isHeld( portNumber ) ) {
			if( System.currentTimeMillis() >= deadline ) {
				return false;
			}

			try {
				Thread.sleep( 200 );
			}
			catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
				return false;
			}
		}

		return true;
	}

	private static boolean isHeld( final int portNumber ) {
		try( Socket socket = new Socket()) {
			socket.connect( new InetSocketAddress( "localhost", portNumber ), 250 );
			return true;
		}
		catch( IOException e ) {
			return false;
		}
	}
}
