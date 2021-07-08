package ng.adaptor.raw;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

/**
 * Experimental raw socket adaptor
 */

public class NGAdaptorRaw extends NGAdaptor {

	private static class Properties {
		private static int port = 1200;
		private static int workerThreadCount = 4;
	}

	private static final String CRLF = "\r\n";
	private static Logger logger = LoggerFactory.getLogger( NGAdaptorRaw.class );

	private static LongAdder numberOfRequestsServed = new LongAdder();

	@Override
	public void start() {
		final ExecutorService es = Executors.newFixedThreadPool( Properties.workerThreadCount );
		new Thread( () -> {

			try( final ServerSocket serverSocket = new ServerSocket( Properties.port ) ;) {
				logger.info( "Started listening for connections on port {}", Properties.port );
				while( true ) {
					final Socket clientSocket = serverSocket.accept();
					//					clientSocket.setTcpNoDelay( true ); // We're not totally sure if we want to disable Nagle's algorithm.
					es.execute( new WorkerThread( clientSocket ) );
					numberOfRequestsServed.increment();
					logger.info( "Served requests: {}", numberOfRequestsServed );
				}
			}
			catch( final Exception e ) {
				// FIXME: Actually handle this exception
				e.printStackTrace();
				throw new RuntimeException( e );
			}
		}, "Listener" ).start();
	}

	public static class WorkerThread implements Runnable {

		private final Socket _clientSocket;

		public WorkerThread( final Socket clientSocket ) {
			_clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try {
				final NGRequest request = requestFromInputStream( _clientSocket.getInputStream() );
				final NGResponse response = NGApplication.application().dispatchRequest( request );

				writeResponseToStream( response, _clientSocket.getOutputStream() );

				// Closing the socket also closes it's inputstream and outputstream
				// FIXME: finally clause?
				_clientSocket.close();
			}
			catch( final IOException e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * FIXME: We're currently writing directly to the stream. Some sort of buffered writing would probably be more appropriate
	 */
	private static void writeResponseToStream( final NGResponse response, OutputStream stream ) {
		Objects.requireNonNull( response );
		Objects.requireNonNull( stream );

		try {
			stream = new BufferedOutputStream( stream, 32000 );
			write( stream, response.httpVersion() + " " + response.status() + " OK" ); // FIXME: Don't just write OK here!
			write( stream, CRLF ); // FIXME: Do we need CRLF?

			// FIXME: Currently only writing out the first header value
			for( final Entry<String, List<String>> entry : response.headers().entrySet() ) {
				write( stream, entry.getKey() );
				write( stream, ": " );
				write( stream, entry.getValue().get( 0 ) );
				write( stream, CRLF );
			}

			write( stream, CRLF );
			stream.write( response.contentBytes() );
			stream.flush();
		}
		catch( final IOException e ) {
			// FIXME: Actually handle this exception
			e.printStackTrace();
		}
	}

	private static void write( OutputStream stream, final String string ) {
		Objects.requireNonNull( stream );
		Objects.requireNonNull( string );

		try {
			stream.write( string.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch( final IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @returns An NGRequest parsed from the InputStream
	 */
	private static NGRequest requestFromInputStream( final InputStream stream ) {
		Objects.requireNonNull( stream );

		try {
			// FIXME: We should not be using a BufferedReader. Those *suck*
			// FIXME: This hardcoded encoding is really hardcoded, isn't it?
			final BufferedReader in = new BufferedReader( new InputStreamReader( stream, StandardCharsets.UTF_8 ) );

			String method = null;
			String uri = null;
			String httpVersion = null;
			final Map<String, List<String>> headers = new HashMap<>();
			byte[] content = null;

			int currentLineNumber = 0;

			String line = in.readLine();

			while( line.length() > 0 ) {
				logger.info( "Parsing request line {} : {}", currentLineNumber, line );

				if( currentLineNumber++ == 0 ) {
					final String[] values = line.split( " " );
					method = values[0];
					uri = values[1];
					httpVersion = values[2];
				}
				else {
					final int colonIndex = line.indexOf( ":" );

					// FIXME: Handle multiple same-name headers
					// FIXME: Handle multiple header values
					if( colonIndex > -1 ) {
						String headerName = line.substring( 0, colonIndex );
						headerName = headerName.toLowerCase(); // FIXME: We want headers to be case preserving. This is currently a hack.

						final String headerValueString = line.substring( colonIndex + 1 ).trim(); // FIXME: Not sure if trimming is the right thing to do here

						headers.put( headerName, Arrays.asList( headerValueString ) );
					}
				}

				line = in.readLine();
			}

			// We have to know the value of the content-length header to parse the content
			final List<String> contentLengthHeaderValues = headers.get( "content-length" );

			if( contentLengthHeaderValues != null ) {
				final int contentLength = Integer.parseInt( contentLengthHeaderValues.get( 0 ) );

				// FIXME: Non-string request content is not just for losers. But for testing purposes, we're just handling string requests.
				//			String bodyLine = in.readLine();
				final char[] contentBuffer = new char[contentLength];
				in.read( contentBuffer, 0, contentLength );

				// Not encoding here. Should be based on a setting
				content = String.valueOf( contentBuffer ).getBytes( StandardCharsets.UTF_8 );
			}
			else {
				// FIXME
				logger.warn( "I didn't find a content-length header, so I'm not parsing any content. Just so you know it" );
				content = new byte[0];
			}

			return new NGRequest( method, uri, httpVersion, headers, content );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}
}