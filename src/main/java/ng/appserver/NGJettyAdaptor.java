package ng.appserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

			// This is where the application logic will perform it's actual work 
			final NGRequest woRequest = servletRequestToNGRequest( servletRequest );
			final NGResponse ngResponse = NGApplication.application().dispatchRequest( woRequest );

			// FIXME Handles a String response only
			final ByteBuffer content = ByteBuffer.wrap( ngResponse.bytes() );

			final AsyncContext async = servletRequest.startAsync();
			final ServletOutputStream out = servletResponse.getOutputStream();

			out.setWriteListener( new WriteListener() {
				@Override
				public void onWritePossible() throws IOException {
					while( out.isReady() ) {
						if( !content.hasRemaining() ) {
							servletResponse.setStatus( ngResponse.status() );

							for( final Entry<String, List<String>> entry : ngResponse.headers().entrySet() ) {
								for( final String headerValue : entry.getValue() ) {
									servletResponse.addHeader( entry.getKey(), headerValue );
								}
							}

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

	private static NGRequest servletRequestToNGRequest( final HttpServletRequest sr ) {
		// FIXME: We're not passing in the request parameters
		// FIXME: WE need to read the request's content as well
		final NGRequest request = new NGRequest( sr.getMethod(), sr.getRequestURI(), headerMap( sr ), null );
		return request;
	}

	/**
	 * FIXME: Implement
	 */
	private static Map<String,List<String>> headerMap( final HttpServletRequest sr ) {
		final Map<String,List<String>> map = new HashMap<>();
		/*
		
		while( sr.getHeaderNames().hasMoreElements() ) {
			final String headerName = sr.getHeaderNames().nextElement();
			sr.getHeaderNames();
		}

		while( enumeration.hasMoreElements() ) {
			map.get( enumeration.nextElement() )
		}
		*/
		return map;
	}
}