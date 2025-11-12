package ng.xperimental;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.NGResponseMultipart;
import ng.appserver.privates.NGDevelopmentInstanceStopper;

/**
 * Experimental webserver adaptor using Java's built in HTTP server.
 *
 * FIXME: Work in progress. Not sure this will ever be used, but our performance metrics show it delivers
 */

public class NGAdaptorPlain extends NGAdaptor {

	private static final Logger logger = LoggerFactory.getLogger( NGAdaptorPlain.class );

	private NGApplication _application;

	/**
	 * Port used if no port number is specified in properties
	 *
	 * FIXME: This should be a default for all adaptors // Hugi 2021-12-31
	 */
	private static final int DEFAULT_PORT_NUMBER = 1200;

	@Override
	public void start( NGApplication application ) {
		_application = application;

		Integer port = application.properties().d().propWOPort(); // FIXME: Ugly way to get the port number

		if( port == null ) {
			logger.warn( "port property is not set, defaulting to port {}", DEFAULT_PORT_NUMBER );
			port = DEFAULT_PORT_NUMBER;
		}

		try {
			var server = HttpServer.create( new InetSocketAddress( port ), 0 );
			server.setExecutor( Executors.newVirtualThreadPerTaskExecutor() );
			server.createContext( "/" ).setHandler( new NGHandler() );
			server.start();
			application.adaptorListening();
		}
		catch( final Exception e ) {
			if( application.isDevelopmentMode() && e instanceof BindException ) {
				logger.info( "Our port seems to be in use and we're in development mode. Let's try murdering the bastard that's blocking us" );
				NGDevelopmentInstanceStopper.stopPreviousDevelopmentInstance( port );
				start( application );
			}
			else {
				// FIXME: Handle this a bit more gracefully perhaps? // Hugi 2021-11-20
				e.printStackTrace();
				System.exit( -1 );
			}
		}
	}

	public class NGHandler implements HttpHandler {

		@Override
		public void handle( HttpExchange exchange ) throws IOException {

			final String contentType = exchange.getRequestHeaders().getFirst( "content-type" );

			final NGRequest ngRequest;

			if( contentType != null && contentType.contains( "multipart/form-data" ) ) {
				throw new NotYetImplementedException();
				//				ngRequest = multipartRequestToNGRequest( jettyRequest, contentType );
			}
			else {
				ngRequest = requestToNGRequest( exchange );
			}

			// This is where the application logic will perform it's actual work
			final NGResponse ngResponse = _application.dispatchRequest( ngRequest );

			for( final NGCookie c : ngResponse.cookies() ) {
				setCookie( exchange, c.name(), c.value(), c.maxAge(), c.isHttpOnly(), c.isSecure(), c.sameSite() );
			}

			exchange.getResponseHeaders().putAll( ngResponse.headers() );

			if( ngResponse instanceof NGResponseMultipart mp ) {
				throw new NotYetImplementedException();
			}
			else if( ngResponse.contentInputStream() != null ) {
				final long contentLength = ngResponse.contentInputStreamLength();

				if( contentLength == -1 ) {
					throw new IllegalArgumentException( "NGResponse.contentInputStream() is set but contentInputLength has not been set. You must provide the content length when serving an InputStream" );
				}

				exchange.sendResponseHeaders( ngResponse.status(), contentLength );

				try( final InputStream inputStream = ngResponse.contentInputStream()) {
					try( final OutputStream out = exchange.getResponseBody()) {
						inputStream.transferTo( out );
					}
				}
			}
			else {
				exchange.sendResponseHeaders( ngResponse.status(), ngResponse.contentBytesLength() );

				try( final OutputStream out = exchange.getResponseBody()) {
					ngResponse.contentByteStream().writeTo( out );
				}
			}
		}

		/**
		 * @return the given Request converted to an NGRequest
		 */
		private static NGRequest requestToNGRequest( final HttpExchange exchange ) {

			// We read the formValues map before reading the requests content stream, since consuming the content stream will remove POST parameters
			final Map<String, List<String>> formValues = new HashMap<>();

			final String queryString = exchange.getRequestURI().getRawQuery();

			if( queryString != null ) {
				parseParams( queryString, formValues );
			}

			// Create a stream for the request's content (that we might not read into, actually
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();

			final String method = exchange.getRequestMethod();
			final String contentType = Optional.ofNullable( exchange.getRequestHeaders().getFirst( "Content-Type" ) ).orElse( "" );

			try( InputStream is = exchange.getRequestBody()) {
				// If POST or PUT and content-type is form-urlencoded, read the body
				if( ("POST".equalsIgnoreCase( method ) || "PUT".equalsIgnoreCase( method )) && contentType.startsWith( "application/x-www-form-urlencoded" ) ) {
					String body = new String( is.readAllBytes(), StandardCharsets.UTF_8 );
					parseParams( body, formValues );
				}
				else {
					// FIXME: we're always consuming the request's entire body at this point. Allowing us to pass in a stream would be... sensible // Hugi 2025-06-09
					is.transferTo( bos );
				}
			}
			catch( IOException e ) {
				throw new UncheckedIOException( "Failed to consume the HTTP request's inputstream", e );
			}

			final String uri = exchange.getRequestURI().toString();
			final String httpVersion = exchange.getProtocol();
			final Map<String, List<String>> headers = exchange.getRequestHeaders();
			final Map<String, List<String>> cookieValues = cookieValues( exchange );
			final byte[] content = bos.toByteArray();

			return new NGRequest( method, uri, httpVersion, headers, formValues, cookieValues, content );
		}

		/**
		 * Parse a query string (name=value&name2=value2 etc.)
		 */
		private static void parseParams( String data, Map<String, List<String>> params ) {

			for( String pair : data.split( "&" ) ) {
				if( pair.isEmpty() ) {
					continue;
				}

				int idx = pair.indexOf( '=' );
				final String key = idx > 0 ? decode( pair.substring( 0, idx ) ) : decode( pair );
				final String value = idx > 0 ? decode( pair.substring( idx + 1 ) ) : "";
				params.computeIfAbsent( key, k -> new ArrayList<>() ).add( value );
			}
		}

		private static String decode( String s ) {
			return URLDecoder.decode( s, StandardCharsets.UTF_8 );
		}

		/**
		 * @return The listed cookies as a map
		 *
		 * FIXME: We're not properly constructing the map; might fail for cookies with multiple values
		 */
		private static Map<String, List<String>> cookieValues( final HttpExchange exchange ) {
			final Map<String, List<String>> cookies = new HashMap<String, List<String>>();

			final String cookieHeader = exchange.getRequestHeaders().getFirst( "Cookie" );

			if( cookieHeader != null ) {
				final String[] pairs = cookieHeader.split( ";\\s*" );

				for( String pair : pairs ) {
					int eq = pair.indexOf( '=' );
					if( eq > 0 ) {
						final String name = URLDecoder.decode( pair.substring( 0, eq ), StandardCharsets.UTF_8 );
						final String value = URLDecoder.decode( pair.substring( eq + 1 ), StandardCharsets.UTF_8 );
						cookies.put( name, List.of( value ) );
					}
				}
			}

			return cookies;
		}

		/** Sets a cookie header with optional attributes. */
		public static void setCookie( HttpExchange exchange, String name, String value, int maxAgeSeconds, boolean httpOnly, boolean secure, String sameSite ) {

			final StringBuilder sb = new StringBuilder();
			sb.append( URLEncoder.encode( name, StandardCharsets.UTF_8 ) ).append( '=' ).append( URLEncoder.encode( value, StandardCharsets.UTF_8 ) );

			if( maxAgeSeconds >= 0 ) {
				sb.append( "; Max-Age=" ).append( maxAgeSeconds );
			}

			sb.append( "; Path=/" );

			if( secure ) {
				sb.append( "; Secure" );
			}

			if( httpOnly ) {
				sb.append( "; HttpOnly" );
			}

			if( sameSite != null && !sameSite.isBlank() ) {
				sb.append( "; SameSite=" ).append( sameSite );
			}

			exchange.getResponseHeaders().add( "Set-Cookie", sb.toString() );
		}
	}

	public static class NotYetImplementedException extends RuntimeException {

		public NotYetImplementedException() {
			super( "Not yet implemented" );
		}
	}
}