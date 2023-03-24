package ng.adaptor.jetty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates._NGUtilities;

public class NGAdaptorJetty extends NGAdaptor {

	private static final Logger logger = LoggerFactory.getLogger( NGAdaptorJetty.class );

	/**
	 * Port used if no port number is specified in properties
	 *
	 * FIXME: This should be a default for all adaptors // Hugi 2021-12-31
	 */
	private static final int DEFAULT_PORT_NUMBER = 1200;

	private Server server;

	@Override
	public void start( NGApplication application ) {
		final int minThreads = 8;
		final int maxThreads = 32;
		final int idleTimeout = 2000; // Specified in milliseconds

		Integer port = application.properties().propWOPort(); // FIXME: Ugly way to get the port number

		if( port == null ) {
			logger.warn( "port property is not set, defaulting to port {}", DEFAULT_PORT_NUMBER );
			port = DEFAULT_PORT_NUMBER;
		}

		final QueuedThreadPool threadPool = new QueuedThreadPool( maxThreads, minThreads, idleTimeout );

		server = new Server( threadPool );
		final ServerConnector connector = new ServerConnector( server );
		connector.setPort( port );
		server.setConnectors( new Connector[] { connector } );

		final ServletHandler servletHandler = new ServletHandler();
		server.setHandler( servletHandler );

		ServletHolder holder = new ServletHolder();
		holder.setServlet( new NGServlet( application ) );
		servletHandler.addServletWithMapping( holder, "/" );

		try {
			server.start();
		}
		catch( final Exception e ) {
			if( application.properties().isDevelopmentMode() && e instanceof IOException && e.getCause() instanceof BindException ) {
				logger.info( "Our port seems to be in use and we're in development mode. Let's try murdering the bastard that's blocking us" );
				_NGUtilities.stopPreviousDevelopmentInstance( port );
				start( application );
			}
			else {
				// FIXME: Handle this a bit more gracefully perhaps? // Hugi 2021-11-20
				e.printStackTrace();
				System.exit( -1 );
			}
		}
	}

	public void stop() throws Exception {
		server.stop();
	}

	public static class NGServlet extends HttpServlet {

		private NGApplication _application;

		public NGServlet( NGApplication application ) {
			Objects.requireNonNull( application );
			_application = application;
		}

		@Override
		protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
			doRequest( request, response );
		}

		@Override
		protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
			doRequest( request, response );
		}

		private void doRequest( final HttpServletRequest servletRequest, final HttpServletResponse servletResponse ) throws ServletException, IOException {

			// This is where the application logic will perform it's actual work
			final NGRequest woRequest = servletRequestToNGRequest( servletRequest );
			final NGResponse ngResponse = _application.dispatchRequest( woRequest );

			servletResponse.setStatus( ngResponse.status() );

			// FIXME: Thoughts on content-length:
			// - Should we always be setting the content length to zero?
			// - Should we complain if a content stream has been set, but contentInputStreamLength not?
			// Hugi 2023-01-26
			final long contentLength;

			if( ngResponse.contentInputStream() != null ) {
				// If an inputstream is present, use the stream's manually specified length value
				contentLength = ngResponse.contentInputStreamLength();
			}
			else {
				// Otherwise we go for the length of the response's contained data/bytes.
				contentLength = ngResponse.contentBytesLength();
			}

			servletResponse.setHeader( "content-length", String.valueOf( contentLength ) );

			for( final NGCookie ngCookie : ngResponse.cookies() ) {
				servletResponse.addCookie( ngCookieToServletCookie( ngCookie ) );
			}

			for( final Entry<String, List<String>> entry : ngResponse.headers().entrySet() ) {
				for( final String headerValue : entry.getValue() ) {
					servletResponse.addHeader( entry.getKey(), headerValue );
				}
			}

			try( final OutputStream out = servletResponse.getOutputStream()) {
				if( ngResponse.contentInputStream() != null ) {
					try( final InputStream inputStream = ngResponse.contentInputStream()) {
						// FIXME: We should probably be doing some buffering // Hugi 2023-01-26
						inputStream.transferTo( out );
					}
				}
				else {
					ngResponse.contentByteStream().writeTo( out );
				}
			}
		}
	}

	private static Cookie ngCookieToServletCookie( final NGCookie ngCookie ) {
		final Cookie servletCookie = new Cookie( ngCookie.name(), ngCookie.value() );

		servletCookie.setVersion( 1 );

		if( ngCookie.domain() != null ) {
			servletCookie.setDomain( ngCookie.domain() );
		}

		if( ngCookie.path() != null ) {
			servletCookie.setPath( ngCookie.path() );
		}

		servletCookie.setHttpOnly( ngCookie.isHttpOnly() );
		servletCookie.setSecure( ngCookie.isSecure() );

		if( ngCookie.comment() != null ) {
			servletCookie.setComment( ngCookie.comment() );
		}

		if( ngCookie.maxAge() != null ) {
			servletCookie.setMaxAge( ngCookie.maxAge() );
		}

		//		FIXME: The setAttribute() API was added in Servlet API 6.0, so this will have to wait for Jetty 12 (or for Jetty 11 to support an updated servlet spec) // Hugi 2023-02-06
		//		if( ngCookie.sameSite() != null ) {
		//			servletCookie.setAttribute( "SameSite", ngCookie.sameSite() );
		//		}

		return servletCookie;
	}

	/**
	 * @return the given HttpServletRequest converted to an NGRequest
	 */
	private static NGRequest servletRequestToNGRequest( final HttpServletRequest sr ) {

		// We read the formValues map before reading the requests content stream, since consuming the content stream will remove POST parameters
		final Map<String, List<String>> formValuesFromServletRequest = formValues( sr.getParameterMap() );

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try( final InputStream is = sr.getInputStream()) {
			is.transferTo( bos );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( "Failed to consume the HTTP request's inputstream", e );
		}

		final NGRequest request = new NGRequest( sr.getMethod(), sr.getRequestURI(), sr.getProtocol(), headerMap( sr ), bos.toByteArray() );

		// FIXME: Form value parsing should really happen within the request object, not in the adaptor // Hugi 2021-12-31
		request._setFormValues( formValuesFromServletRequest );

		// FIXME: Cookie parsing should happen within the request object, not in the adaptor // Hugi 2021-12-31
		request._setCookieValues( cookieValues( sr.getCookies() ) );

		return request;
	}

	/**
	 * @return The queryParameters as a formValue Map (our format)
	 */
	private static Map<String, List<String>> formValues( Map<String, String[]> queryParameters ) {

		Map<String, List<String>> map = new HashMap<>();

		for( Entry<String, String[]> entry : queryParameters.entrySet() ) {
			map.put( entry.getKey(), Arrays.asList( entry.getValue() ) );
		}

		return map;
	}

	/**
	 * @return The listed cookies as a map
	 */
	private static Map<String, List<String>> cookieValues( final Cookie[] cookies ) {
		final Map<String, List<String>> cookieValues = new HashMap<>();

		if( cookies != null ) {
			for( Cookie cookie : cookies ) {
				List<String> list = cookieValues.get( cookie.getName() );

				if( list == null ) {
					list = new ArrayList<>();
					cookieValues.put( cookie.getName(), list );
				}

				list.add( cookie.getValue() );
			}
		}

		return cookieValues;
	}

	/**
	 * @return The headers from the ServletRequest as a Map
	 */
	private static Map<String, List<String>> headerMap( final HttpServletRequest servletRequest ) {
		final Map<String, List<String>> map = new HashMap<>();

		final Enumeration<String> headerNamesEnumeration = servletRequest.getHeaderNames();

		while( headerNamesEnumeration.hasMoreElements() ) {
			final String headerName = headerNamesEnumeration.nextElement();
			final List<String> values = new ArrayList<>();
			map.put( headerName, values );

			final Enumeration<String> headerValuesEnumeration = servletRequest.getHeaders( headerName );

			while( headerValuesEnumeration.hasMoreElements() ) {
				values.add( headerValuesEnumeration.nextElement() );
			}
		}

		return map;
	}
}