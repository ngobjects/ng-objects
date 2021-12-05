package ng.adaptor.jetty.http2;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.URL;
import java.util.EnumSet;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.PushCacheFilter;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.PushBuilder;
import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;

public class NGAdaptorJettyHTTP2 extends NGAdaptor {

	private static final Logger logger = LoggerFactory.getLogger( NGAdaptorJettyHTTP2.class );

	private Server server;

	@Override
	public void start() {
		final int minThreads = 8;
		final int maxThreads = 32;
		final int idleTimeout = 2000; // Specified in milliseconds
		final int port = 1200;

		final QueuedThreadPool threadPool = new QueuedThreadPool( maxThreads, minThreads, idleTimeout );

		server = new Server( threadPool );
		final ServerConnector connector = new ServerConnector( server );
		connector.setPort( port );
		server.setConnectors( new Connector[] { connector } );

		final ServletHandler servletHandler = new ServletHandler();
		server.setHandler( servletHandler );

		servletHandler.addServletWithMapping( NGServlet.class, "/" );
		servletHandler.addFilterWithMapping( PushCacheFilter.class, "/", EnumSet.of( DispatcherType.REQUEST ) );

		try {
			server.start();
		}
		catch( final Exception e ) {
			if( NGApplication.isDevelopmentMode() && e instanceof IOException && e.getCause() instanceof BindException ) {
				logger.info( "Our port seems to be in use and we're in development mode. Let's try murdering the bastard that's blocking us" );
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
		protected void doGet( HttpServletRequest req, HttpServletResponse resp ) {

			PushBuilder pushBuilder = req.newPushBuilder();
			//		PushBuilder pushBuilder = Request.getBaseRequest( req ).newPushBuilder();

			pushBuilder
					.path( "images/kodedu-logo.png" )
					.addHeader( "content-type", "image/png" )
					.push();

			try( PrintWriter respWriter = resp.getWriter() ;) {
				respWriter.write( "<html>" +
						"<img src='images/kodedu-logo.png'>" +
						"</html>" );
			}
			catch( IOException e ) {
				throw new RuntimeException( e );
			}
		}
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