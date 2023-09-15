package ng.adaptor.raw;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGDevelopmentInstanceStopper;

/**
 * This is an extremely experimental adaptor that evolves with the framework to support only the exact HTTP features required by it.
 *
 * This document will probably be our best friend throughout this journey: https://www.ietf.org/rfc/rfc2616.txt
 */

public class NGAdaptorRaw extends NGAdaptor {

	private static Logger logger = LoggerFactory.getLogger( NGAdaptorRaw.class );
	private static final String CRLF = "\r\n";

	@Override
	public void start( NGApplication application ) {
		// CHECKME: Properties should be loaded from NGProperties // Hugi 2021-12-29
		final int port = 1200;
		final int workerThreadCount = 4;

		final ExecutorService es = Executors.newFixedThreadPool( workerThreadCount );
		new Thread( () -> {
			try( final ServerSocket serverSocket = new ServerSocket( port ) ;) {
				logger.info( "Started listening for connections on port {}", port );
				while( true ) {
					final Socket clientSocket = serverSocket.accept();
					//					clientSocket.setTcpNoDelay( true ); // We're not totally sure if we want to disable Nagle's algorithm.
					es.execute( new WorkerThread( clientSocket, application ) );
				}
			}
			catch( final Exception e ) {
				if( application.properties().isDevelopmentMode() && e instanceof BindException ) {
					logger.info( "Our port seems to be in use and we're in development mode. Let's try murdering the bastard that's blocking us" );
					NGDevelopmentInstanceStopper.stopPreviousDevelopmentInstance( port );
					start( application );
				}
				else {
					// CHECKME: Handle a bit more gracefully // Hugi 2021-11-20
					e.printStackTrace();
					System.exit( -1 );
				}
			}
		}, "Listener" ).start();
	}

	public static class WorkerThread implements Runnable {

		private final Socket _clientSocket;
		private final NGApplication _application;

		public WorkerThread( final Socket clientSocket, final NGApplication application ) {
			Objects.requireNonNull( clientSocket );
			Objects.requireNonNull( application );
			_clientSocket = clientSocket;
			_application = application;
		}

		@Override
		public void run() {
			try {
				final NGRequest request = requestFromInputStream( _clientSocket.getInputStream() );
				final NGResponse response = _application.dispatchRequest( request );

				writeResponseToStream( response, _clientSocket.getOutputStream() );
				_clientSocket.close();
			}
			catch( final IOException e ) {
				throw new RuntimeException( e );
			}
		}
	}

	private static void writeResponseToStream( final NGResponse response, OutputStream stream ) {
		Objects.requireNonNull( response );
		Objects.requireNonNull( stream );

		try {
			stream = new BufferedOutputStream( stream, 32000 );
			write( stream, response.httpVersion() + " " + response.status() /* + " OK" */ ); // CHECKME: We might want to consider a reason phrase
			write( stream, CRLF );

			for( final Entry<String, List<String>> entry : response.headers().entrySet() ) {
				for( final String headerValue : entry.getValue() ) {
					write( stream, entry.getKey() );
					write( stream, ": " );
					write( stream, headerValue );
					write( stream, CRLF );
				}
			}

			write( stream, CRLF );
			stream.write( response.contentBytes() );
			stream.flush();
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	private static void write( OutputStream stream, final String string ) {
		Objects.requireNonNull( stream );
		Objects.requireNonNull( string );

		try {
			stream.write( string.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * @returns An NGRequest parsed from the InputStream
	 */
	private static NGRequest requestFromInputStream( final InputStream stream ) {
		Objects.requireNonNull( stream );

		try {
			// CHECKME: We should not be using a BufferedReader
			// CHECKME: This hardcoded encoding is really hardcoded, isn't it?
			final BufferedReader in = new BufferedReader( new InputStreamReader( stream, StandardCharsets.UTF_8 ) );

			String method = null;
			String uri = null;
			String httpVersion = null;
			final Map<String, List<String>> headers = new HashMap<>();
			byte[] content = null;

			int currentLineNumber = 0;

			String line = in.readLine();

			while( line != null && line.length() > 0 ) {
				logger.debug( "Parsing request line {} : {}", currentLineNumber, line );

				if( currentLineNumber++ == 0 ) {
					final String[] values = line.split( " " );
					method = values[0];
					uri = values[1];
					httpVersion = values[2];
				}
				else {
					final int colonIndex = line.indexOf( ":" );

					// CHECKME: Handle multiple same-name headers
					if( colonIndex > -1 ) {
						final String headerName = line.substring( 0, colonIndex );
						String headerValueString = line.substring( colonIndex + 1 ).trim();
						headerValueString = headerValueString.trim(); // RFC 2616 tells us leading/trailing whitespace is insignificant.
						headers.put( headerName, Arrays.asList( headerValueString ) );
					}
				}

				line = in.readLine();
			}

			// We have to know the value of the content-length header to parse the content
			final List<String> contentLengthHeaderValues = headers.get( "content-length" );

			if( contentLengthHeaderValues != null ) {
				final int contentLength = Integer.parseInt( contentLengthHeaderValues.get( 0 ) );

				if( contentLength > 0 ) {
					// CHECKME: Handle non-string request content
					// String bodyLine = in.readLine();
					final char[] contentBuffer = new char[contentLength];
					in.read( contentBuffer, 0, contentLength );

					// Not encoding here. Should be based on a setting
					content = String.valueOf( contentBuffer ).getBytes( StandardCharsets.UTF_8 );
				}
				else {
					content = new byte[0];
				}
			}
			else {
				logger.debug( "I didn't find a content-length header, so I'm not parsing any content. Just so you know it" ); // CHECKME: We're going to want to look into this // Hugi 2022-06-04
				content = new byte[0];
			}

			return new NGRequest( method, uri, httpVersion, headers, content );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}
}