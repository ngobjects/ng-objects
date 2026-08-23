package ng.dev;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A development aid that mirrors the application's console output (System.out and System.err)
 * into a bounded in-memory ring buffer, so the most recent lines can be read back over HTTP
 * (see the /ng/dev/log route in NGDevelopmentPlugin).
 *
 * Why this exists: when an external tool (a script, or an AI agent working in the repo) drives
 * the app, it'll commonly add temporary logging and then need to read the result. Without this,
 * that means a human copying the IDE console by hand. With it, the output is one HTTP fetch away.
 *
 * Capturing by teeing the streams is safe here — unlike in WO/wonder-slim, where the
 * NSLog/log4j topology pipes the streams back into the logging system and a tee creates a
 * feedback loop. In ng the logging backend (e.g. slf4j-simple) just writes *to* the streams,
 * so a write-through tee sees each line exactly once. It also composes with the framework's
 * own WOOutputPath redirection, as long as it's installed after that redirect has happened
 * (which it is — installation happens at plugin load, redirection in main()).
 */

public class NGConsoleCapture {

	/**
	 * Most recent lines retained. Bounded so memory stays flat over long sessions.
	 */
	private static final int MAX_LINES = 2000;

	/**
	 * The ring buffer of recent lines. Guarded by itself.
	 */
	private static final Deque<String> _lines = new ArrayDeque<>( MAX_LINES );

	private static volatile boolean _installed;

	private NGConsoleCapture() {}

	/**
	 * Installs the console tee on System.out and System.err. Idempotent — a second call is a
	 * no-op, so repeated initialization can't stack tees and duplicate every captured line.
	 */
	public static synchronized void install() {
		if( _installed ) {
			return;
		}

		System.setOut( new PrintStream( new TeeOutputStream( System.out ), true, StandardCharsets.UTF_8 ) );
		System.setErr( new PrintStream( new TeeOutputStream( System.err ), true, StandardCharsets.UTF_8 ) );
		_installed = true;
	}

	/**
	 * @return a snapshot of the captured lines (oldest first), optionally filtered to lines
	 *         containing [contains] (case-sensitive, ignored when null or empty) and limited to
	 *         the last [tail] matching lines (ignored when zero or negative).
	 */
	public static List<String> snapshot( final String contains, final int tail ) {
		final List<String> all;

		synchronized( _lines ) {
			all = new ArrayList<>( _lines );
		}

		List<String> result = all;

		if( contains != null && !contains.isEmpty() ) {
			result = new ArrayList<>();

			for( final String line : all ) {
				if( line.contains( contains ) ) {
					result.add( line );
				}
			}
		}

		if( tail > 0 && result.size() > tail ) {
			result = new ArrayList<>( result.subList( result.size() - tail, result.size() ) );
		}

		return result;
	}

	private static void append( final String line ) {
		synchronized( _lines ) {
			if( _lines.size() >= MAX_LINES ) {
				_lines.removeFirst();
			}
			_lines.addLast( line );
		}
	}

	/**
	 * An OutputStream that writes through to a delegate (the original console stream) and also
	 * accumulates bytes until a newline, appending each completed line to the ring buffer.
	 *
	 * The tee deliberately lives at the OutputStream level, wrapped in a plain PrintStream by
	 * install() — NOT as a PrintStream subclass with overridden write methods. PrintStream's
	 * string-printing methods route through internal writer machinery whose exact write paths
	 * are JDK-version-dependent, so a subclass can't reliably intercept everything; an
	 * underlying OutputStream is the one choke point every byte is guaranteed to pass through.
	 *
	 * Line assembly is per-stream; interleaving between out/err is acceptable for a dev log view.
	 */
	private static class TeeOutputStream extends OutputStream {

		private final OutputStream _delegate;
		private final ByteArrayOutputStream _lineBuffer = new ByteArrayOutputStream( 256 );

		private TeeOutputStream( final OutputStream delegate ) {
			_delegate = delegate;
		}

		@Override
		public void write( final int b ) throws IOException {
			_delegate.write( b );
			capture( b );
		}

		@Override
		public void write( final byte[] buf, final int off, final int len ) throws IOException {
			_delegate.write( buf, off, len );

			for( int i = 0; i < len; i++ ) {
				capture( buf[off + i] );
			}
		}

		@Override
		public void flush() throws IOException {
			_delegate.flush();
		}

		private synchronized void capture( final int b ) {
			if( b == '\n' ) {
				final String line = _lineBuffer.toString( StandardCharsets.UTF_8 );
				_lineBuffer.reset();
				append( line );
			}
			else if( b != '\r' ) {
				_lineBuffer.write( b );
			}
		}
	}
}
