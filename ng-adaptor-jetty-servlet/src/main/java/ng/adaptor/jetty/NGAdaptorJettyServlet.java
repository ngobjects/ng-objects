package ng.adaptor.jetty;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.websocket.server.ServerEndpointConfig;
import ng.adaptor.jetty.experimental.NGWebSocketEndpoint;
import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;
import ng.appserver.privates.NGDevelopmentInstanceStopper;

public class NGAdaptorJettyServlet extends NGAdaptor {

	private static final Logger logger = LoggerFactory.getLogger( NGAdaptorJettyServlet.class );

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

		Integer port = application.properties().d().propWOPort(); // FIXME: Ugly way to get the port number

		if( port == null ) {
			logger.warn( "port property is not set, defaulting to port {}", DEFAULT_PORT_NUMBER );
			port = DEFAULT_PORT_NUMBER;
		}

		final QueuedThreadPool threadPool = new QueuedThreadPool( maxThreads, minThreads, idleTimeout );

		server = new Server( threadPool );
		final ServerConnector connector = new ServerConnector( server );
		connector.setPort( port );
		server.setConnectors( new Connector[] { connector } );

		final ContextHandlerCollection contexts = new ContextHandlerCollection();
		server.setHandler( contexts );

		final ServletContextHandler servletContextHandler = new ServletContextHandler( "/" );

		final ServletHandler servletHandler = new ServletHandler();
		servletContextHandler.setHandler( servletHandler );
		contexts.addHandler( servletContextHandler );

		final Servlet servlet = new NGServletAdaptor( application );
		final ServletHolder servletHolder = new ServletHolder();
		servletHolder.setServlet( servlet );

		// FIXME: START EXPERIMENTAL MULTIPART HANDLER =======================================================================
		final Path multipartTmpDir = Paths.get( "/Users/hugi/tmp/mp" );
		final String location = multipartTmpDir.toString();

		long maxFileSize = 10 * 1024 * 1024; // 10 MB
		long maxRequestSize = 10 * 1024 * 1024; // 10 MB
		int fileSizeThreshold = 64 * 1024; // 64 KB
		MultipartConfigElement multipartConfig = new MultipartConfigElement( location, maxFileSize, maxRequestSize, fileSizeThreshold );
		servletHolder.getRegistration().setMultipartConfig( multipartConfig );
		// FIXME: END EXPERIMENTAL MULTIPART HANDLER =========================================================================

		servletHandler.addServletWithMapping( servletHolder, "/" );

		// FIXME: Experimental WebSocket support. Currently just for playing around // Hugi 2024-10-10
		JakartaWebSocketServletContainerInitializer.configure( servletContextHandler, ( context, container ) -> {
			ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder.create( NGWebSocketEndpoint.class, "/ng-websocket" ).build();
			container.addEndpoint( endpointConfig );
		} );
		// FIXME: Experimental WebSocket support // Hugi 2024-10-10

		try {
			server.start();
		}
		catch( final Exception e ) {
			if( application.isDevelopmentMode() && e instanceof IOException && e.getCause() instanceof BindException ) {
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

	public void stop() throws Exception {
		server.stop();
	}
}