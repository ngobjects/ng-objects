package ng.adaptor.jetty.http2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
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

/**
 * Looks like a nice example here: https://gist.github.com/ataylor284/7270580d3d46d89585f363f61b773536
 */

public class NGAdaptorJettyHTTP2 extends NGAdaptor {

	private static final Logger logger = LoggerFactory.getLogger( NGAdaptorJettyHTTP2.class );

	private Server server;

	@Override
	public void start() {
		int port = 1200;

		Server server = new Server();

		ServletContextHandler context = new ServletContextHandler( server, "/", ServletContextHandler.SESSIONS );
		context.addServlet( new ServletHolder( new NGServlet() ), "/" );
		server.setHandler( context );

		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme( "https" );

		httpConfig.setSecurePort( port );

		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath( "/Users/hugi/xxxxx-temp/http2_keystore.jks" );
		sslContextFactory.setKeyStorePassword( "password" );
		sslContextFactory.setKeyManagerPassword( "password" );
		sslContextFactory.setCipherComparator( HTTP2Cipher.COMPARATOR );

		// FIXME: Enable this when there's aarch_64 support for Conscrypt: https://github.com/google/conscrypt/issues/1034 // Hugi 2021-12-05
		// sslContextFactory.setProvider( "Conscrypt" );

		HttpConfiguration httpsConfig = new HttpConfiguration( httpConfig );
		httpsConfig.addCustomizer( new SecureRequestCustomizer() );

		HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory( httpsConfig );

		// NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable()
		var alpn = new ALPNServerConnectionFactory();
		alpn.setDefaultProtocol( "h2" );
		SslConnectionFactory ssl = new SslConnectionFactory( sslContextFactory, alpn.getProtocol() );

		var http2Connector = new ServerConnector( server, ssl, alpn, h2, new HttpConnectionFactory( httpsConfig ) );
		http2Connector.setPort( port );
		server.addConnector( http2Connector );

		// ALPN.debug=false // FIXME: Commented this out since there's no available ALPN class // Hugi 2021-12-05

		try {
			server.start();
			server.join();
		}
		catch( Exception e ) {
			e.printStackTrace();
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

		final Cookie[] cookies = sr.getCookies();

		if( cookies != null ) {
			for( Cookie cookie : cookies ) {
				request.addCookie( servletCookieToNGCookie( cookie ) );
			}
		}

		return request;
	}

	private static NGCookie servletCookieToNGCookie( Cookie sc ) {
		return new NGCookie( sc.getName(), sc.getValue(), sc.getDomain(), sc.getPath(), sc.getSecure(), sc.getMaxAge() );
	}

	/**
	 * FIXME: Implement
	 */
	private static Map<String, List<String>> headerMap( final HttpServletRequest sr ) {
		final Map<String, List<String>> map = new HashMap<>();

		final Enumeration<String> headerNamesEnumeration = sr.getHeaderNames();

		while( headerNamesEnumeration.hasMoreElements() ) {
			final String headerName = headerNamesEnumeration.nextElement();
			final List<String> values = new ArrayList<>();
			map.put( headerName, values );

			final Enumeration<String> headerValuesEnumeration = sr.getHeaders( headerName );

			while( headerValuesEnumeration.hasMoreElements() ) {
				values.add( headerValuesEnumeration.nextElement() );
			}
		}

		return map;
	}

	/**
	 * FIXME: This should really live in a more central location and not within just the adaptor // Hugi 2021-11-20
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