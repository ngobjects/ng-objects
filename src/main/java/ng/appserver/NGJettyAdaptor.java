package ng.appserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class NGJettyAdaptor {

	private Server server;

	void run() throws Exception {
		final int maxThreads = 100;
		final int minThreads = 10;
		final int idleTimeout = 120;
		final int port = 1200;

		final QueuedThreadPool threadPool = new QueuedThreadPool( maxThreads, minThreads, idleTimeout );

		server = new Server( threadPool );
		ServerConnector connector = new ServerConnector( server );
		connector.setPort( port );
		server.setConnectors( new Connector[] { connector } );

		ServletHandler servletHandler = new ServletHandler();
		server.setHandler( servletHandler );

		servletHandler.addServletWithMapping( AsyncServlet.class, "/" );

		server.start();
	}

	void stop() throws Exception {
		server.stop();
	}

	public static class AsyncServlet extends HttpServlet {

		protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
			doRequest( request, response );
		}

		protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
			doRequest( request, response );
		}

		private void doRequest( final HttpServletRequest servletRequest, final HttpServletResponse servletResponse ) throws ServletException, IOException {
			final String HEAVY_RESOURCE = "This is some heavy resource that will be served in an async way";

			// This is where the application logic will perform it's actual work 
			final NGRequest woRequest = servletRequestToNGRequest( servletRequest );
			final NGResponse woResponse = NGApplication.application().dispatchRequest( woRequest );

			// FIXME Handles a String response only
			final ByteBuffer content = ByteBuffer.wrap( woResponse.contentString().getBytes( StandardCharsets.UTF_8 ) );

			final AsyncContext async = servletRequest.startAsync();
			final ServletOutputStream out = servletResponse.getOutputStream();

			out.setWriteListener( new WriteListener() {
				@Override
				public void onWritePossible() throws IOException {
					while( out.isReady() ) {
						if( !content.hasRemaining() ) {
							servletResponse.setStatus( woResponse.status() );
							async.complete();
							return;
						}
						out.write( content.get() );
					}
				}

				@Override
				public void onError( Throwable t ) {
					getServletContext().log( "Async Error", t );
					async.complete();
				}
			} );
		}
	}

	private static NGRequest servletRequestToNGRequest( final HttpServletRequest servletRequest ) {
		NGRequest request = new NGRequest();
		request.setURI( servletRequest.getRequestURI() );
		return request;
	}
}