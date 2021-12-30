package ng.adaptor.jetty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
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
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGAdaptorJetty extends NGAdaptor {

	private static final Logger logger = LoggerFactory.getLogger( NGAdaptorJetty.class );

	private Server server;

	@Override
	public void start() {
		final int minThreads = 8;
		final int maxThreads = 32;
		final int idleTimeout = 2000; // Specified in milliseconds

		Integer port = NGApplication.application().properties().propWOPort(); // FIXME: Ugly way to get the port number

		if( port == null ) {
			logger.warn( "port property is not set, defaulting to port 1200" );
			port = 1200;
		}

		final QueuedThreadPool threadPool = new QueuedThreadPool( maxThreads, minThreads, idleTimeout );

		server = new Server( threadPool );
		final ServerConnector connector = new ServerConnector( server );
		connector.setPort( port );
		server.setConnectors( new Connector[] { connector } );

		final ServletHandler servletHandler = new ServletHandler();
		server.setHandler( servletHandler );

		servletHandler.addServletWithMapping( NGServlet.class, "/" );

		try {
			server.start();
		}
		catch( final Exception e ) {
			if( NGApplication.application().properties().isDevelopmentMode() && e instanceof IOException && e.getCause() instanceof BindException ) {
				logger.info( "Our port seems to be in use and we're in development mode. Let's try murdering the bastard that's blocking us" );
				stopPreviousDevelopmentInstance( port );
				start();
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
			final NGResponse ngResponse = NGApplication.application().dispatchRequest( woRequest );

			servletResponse.setStatus( ngResponse.status() );

			servletResponse.setHeader( "content-length", String.valueOf( ngResponse.contentBytes().length ) );

			for( final Entry<String, List<String>> entry : ngResponse.headers().entrySet() ) {
				for( final String headerValue : entry.getValue() ) {
					servletResponse.addHeader( entry.getKey(), headerValue );
				}
			}

			try( final OutputStream out = servletResponse.getOutputStream()) {
				out.write( ngResponse.contentBytes() );
			}
		}
	}

	/**
	 * @return the given HttpServletRequest converted to an NGRequest
	 *
	 * FIXME: We're not passing in the request parameters
	 * FIXME: WE need to read the request's content as well
	 */
	private static NGRequest servletRequestToNGRequest( final HttpServletRequest sr ) {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try( final InputStream is = sr.getInputStream()) {
			is.transferTo( bos );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e ); // FIXME: We're throwing RuntimeExceptions all over the place, we should probably be using NGForwardException, once that's structured
		}

		final NGRequest request = new NGRequest( sr.getMethod(), sr.getRequestURI(), sr.getProtocol(), headerMap( sr ), bos.toByteArray() );

		request.setCookieValues( cookieValues( sr.getCookies() ) );

		return request;
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
	private static Map<String, List<String>> headerMap( final HttpServletRequest sevletRequest ) {
		final Map<String, List<String>> map = new HashMap<>();

		final Enumeration<String> headerNamesEnumeration = sevletRequest.getHeaderNames();

		while( headerNamesEnumeration.hasMoreElements() ) {
			final String headerName = headerNamesEnumeration.nextElement();
			final List<String> values = new ArrayList<>();
			map.put( headerName, values );

			final Enumeration<String> headerValuesEnumeration = sevletRequest.getHeaders( headerName );

			while( headerValuesEnumeration.hasMoreElements() ) {
				values.add( headerValuesEnumeration.nextElement() );
			}
		}

		return map;
	}

	/**
	 * FIXME: This functionality should really be in a more central/generic location, not within the adaptor // Hugi 2021-11-20
	 */
	private static void stopPreviousDevelopmentInstance( int portNumber ) {
		try {
			final String urlString = String.format( "http://localhost:%s/wa/ng.appserver.privates.NGAdminAction/terminate", portNumber );
			new URL( urlString ).openConnection().getContent();
			Thread.sleep( 1000 );
		}
		catch( Throwable e ) {
			logger.info( "Terminated existing development instance" );
		}
	}
}