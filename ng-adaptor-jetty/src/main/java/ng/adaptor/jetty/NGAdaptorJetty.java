package ng.adaptor.jetty;

import java.io.IOException;
import java.nio.ByteBuffer;
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

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
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
		final int port = 1200;

		final QueuedThreadPool threadPool = new QueuedThreadPool( maxThreads, minThreads, idleTimeout );

		server = new Server( threadPool );
		final ServerConnector connector = new ServerConnector( server );
		connector.setPort( port );
		server.setConnectors( new Connector[] { connector } );

		final ServletHandler servletHandler = new ServletHandler();
		server.setHandler( servletHandler );

		servletHandler.addServletWithMapping( AsyncServlet.class, "/" );

		try {
			server.start();
		}
		catch( final Exception e ) {
			// FIXME: Handle this a bit more gracefully perhaps?
			e.printStackTrace();
			System.exit( -1 );
		}
	}

	public void stop() throws Exception {
		server.stop();
	}

	public static class AsyncServlet extends HttpServlet {

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

			// FIXME Handles a String response only
			final ByteBuffer content = ByteBuffer.wrap( ngResponse.contentBytes() );

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

				/**
				 * FIXME: I'm going to assume we have to handle this better
				 */
				@Override
				public void onError( Throwable t ) {
					logger.error( "Error" );
					getServletContext().log( "Async Error", t );
					async.complete();
				}
			} );
		}
	}

	/**
	 * @return the given HttpServletRequest converted to an NGRequest
	 *
	 * FIXME: We're not passing in the request parameters
	 * FIXME: WE need to read the request's content as well
	 */
	private static NGRequest servletRequestToNGRequest( final HttpServletRequest servletRequest ) {
		System.out.println( servletRequest );
		final NGRequest request = new NGRequest( servletRequest.getMethod(), servletRequest.getRequestURI(), servletRequest.getProtocol(), headerMap( servletRequest ), new byte[0] );
		// FIXME: We should not be setting the requests's content to the empty array
		return request;
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
}