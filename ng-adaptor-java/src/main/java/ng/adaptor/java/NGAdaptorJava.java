package ng.adaptor.java;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;
import ng.appserver.NGResponseMultipart;
import ng.appserver.NGResponseMultipart.ContentPart;
import ng.appserver.dev.NGDevelopmentInstanceStopper;
import ng.appserver.http.NGCookie;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGRequests;
import ng.appserver.http.NGResponse;
import ng.appserver.http.NGStandardResponse;
import ng.appserver.http.NGStreamingResponse;

/**
 * An adaptor built on the JDK's own HTTP server, without dependencies. Select it with the property WOAdaptor=ng.adaptor.java.NGAdaptorJava
 *
 * It parses multipart bodies itself (NGMultipartIterator), while the Jetty adaptors use Jetty's parser.
 */

public class NGAdaptorJava extends NGAdaptor {

	private static final Logger logger = LoggerFactory.getLogger( NGAdaptorJava.class );

	/**
	 * Port used if no port number is specified in properties
	 */
	private static final int DEFAULT_PORT_NUMBER = 1200;

	private NGApplication _application;

	@Override
	public void start( final NGApplication application ) {
		_application = application;

		Integer port = application.properties().d().propWOPort();

		if( port == null ) {
			logger.warn( "port property is not set, defaulting to port {}", DEFAULT_PORT_NUMBER );
			port = DEFAULT_PORT_NUMBER;
		}

		try {
			final HttpServer server = HttpServer.create( new InetSocketAddress( port ), 0 );
			server.setExecutor( Executors.newVirtualThreadPerTaskExecutor() );
			server.createContext( "/", this::handle );
			server.start();
			application.adaptorListening();
		}
		catch( final IOException e ) {
			if( application.isDevelopmentMode() && e instanceof BindException ) {
				logger.info( "Our port seems to be in use and we're in development mode. Let's try murdering the bastard that's blocking us" );
				NGDevelopmentInstanceStopper.stopPreviousDevelopmentInstance( port );
				start( application );
			}
			else {
				logger.error( "Couldn't start the adaptor on port {}", port, e );
				System.exit( -1 );
			}
		}
	}

	private void handle( final HttpExchange exchange ) {
		final NGRequest request = request( exchange );

		try {
			// This is where the application logic will perform it's actual work
			final NGResponse response = _application.dispatchRequest( request );
			writeResponse( exchange, response );
		}
		catch( final Exception e ) {
			logger.error( "Couldn't send the response to {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), e );
		}
		finally {
			// Lets the request release what it holds, such as the temporary files of an upload
			request._close();
			exchange.close();
		}
	}

	/**
	 * @return The request the given exchange received. Its body is read, and a multipart body split into parts, when the request asks for it.
	 */
	private static NGRequest request( final HttpExchange exchange ) {
		final URI requestURI = exchange.getRequestURI();
		final String path = requestURI.normalize().getPath();

		return NGRequests.of(
				exchange.getRequestMethod(),
				path == null || path.isEmpty() ? "/" : path,
				requestURI.getRawQuery(),
				exchange.getProtocol(),
				exchange.getRequestHeaders(),
				exchange.getRequestBody(),
				exchange.getRemoteAddress().getAddress().getHostAddress(),
				NGMultipartIterator::new );
	}

	private static void writeResponse( final HttpExchange exchange, final NGResponse response ) throws IOException {

		for( final NGCookie cookie : response.cookies() ) {
			exchange.getResponseHeaders().add( "Set-Cookie", setCookieHeaderValue( cookie ) );
		}

		for( final Entry<String, List<String>> header : response.headers().entrySet() ) {

			// The content length is framing, which the server derives from the body we hand it
			if( !header.getKey().equalsIgnoreCase( "content-length" ) ) {
				exchange.getResponseHeaders().put( header.getKey(), header.getValue() );
			}
		}

		// A HEAD request, and some statuses, never carry a body
		final boolean bodyless = exchange.getRequestMethod().equalsIgnoreCase( "HEAD" ) || response.status() == 204 || response.status() == 304 || response.status() < 200;

		switch( response ) {
			case NGResponseMultipart r -> {
				final byte[] content = multipartContent( r );
				sendBody( exchange, response.status(), bodyless, content.length, new ByteArrayInputStream( content ) );
			}
			case NGStreamingResponse r -> {
				try( final InputStream stream = r.contentInputStream()) {
					sendBody( exchange, response.status(), bodyless, r.contentInputStreamLength(), stream );
				}
			}
			case NGStandardResponse r -> {
				final byte[] content = r.contentBytes();
				sendBody( exchange, response.status(), bodyless, content.length, new ByteArrayInputStream( content ) );
			}
			default -> throw new IllegalArgumentException( "Unknown response class: " + response.getClass().getName() );
		}
	}

	/**
	 * Sends the response's status and headers, and the given body of the given length
	 */
	private static void sendBody( final HttpExchange exchange, final int status, final boolean bodyless, final long length, final InputStream body ) throws IOException {
		if( bodyless || length == 0 ) {
			// -1 tells the server there's no body
			exchange.sendResponseHeaders( status, -1 );
			return;
		}

		exchange.sendResponseHeaders( status, length );

		try( final OutputStream out = exchange.getResponseBody()) {
			body.transferTo( out );
		}
	}

	/**
	 * @return The given multipart response's content: each part as form data, between its boundaries
	 */
	private static byte[] multipartContent( final NGResponseMultipart response ) {
		final StringBuilder b = new StringBuilder();

		for( final ContentPart part : response._contentParts.values() ) {
			b.append( "--" ).append( NGResponseMultipart.BOUNDARY ).append( "\r\n" );
			b.append( "Content-Disposition: form-data; name=\"" ).append( part.name() ).append( "\"\r\n\r\n" );
			b.append( part.content() ).append( "\r\n" );
		}

		b.append( "--" ).append( NGResponseMultipart.BOUNDARY ).append( "--\r\n" );
		return b.toString().getBytes( StandardCharsets.UTF_8 );
	}

	/**
	 * @return The value of a Set-Cookie header setting the given cookie (RFC 6265)
	 */
	static String setCookieHeaderValue( final NGCookie cookie ) {
		final StringBuilder b = new StringBuilder();
		b.append( cookie.name() ).append( '=' ).append( cookie.value() );

		if( cookie.domain() != null ) {
			b.append( "; Domain=" ).append( cookie.domain() );
		}

		if( cookie.path() != null ) {
			b.append( "; Path=" ).append( cookie.path() );
		}

		if( cookie.maxAge() != null ) {
			b.append( "; Max-Age=" ).append( cookie.maxAge() );
		}

		if( cookie.secure() ) {
			b.append( "; Secure" );
		}

		if( cookie.httpOnly() ) {
			b.append( "; HttpOnly" );
		}

		if( cookie.sameSite() != null ) {
			b.append( "; SameSite=" ).append( cookie.sameSite().headerValue() );
		}

		return b.toString();
	}
}
