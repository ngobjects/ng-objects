package ng.adaptor.jetty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpCookie.SameSite;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.http.MultiPartFormData;
import org.eclipse.jetty.http.MultiPartFormData.ContentSource;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.Content.Source;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class NGAdaptorJetty extends NGAdaptor {

	private static final Logger logger = LoggerFactory.getLogger( NGAdaptorJetty.class );

	/**
	 * Port used if no port number is specified in properties
	 *
	 * FIXME: This should be a default for all adaptors // Hugi 2021-12-31
	 */
	private static final int DEFAULT_PORT_NUMBER = 1200;

	@Override
	public void start( NGApplication application ) {

		Integer port = application.properties().d().propWOPort(); // FIXME: Ugly way to get the port number

		if( port == null ) {
			logger.warn( "port property is not set, defaulting to port {}", DEFAULT_PORT_NUMBER );
			port = DEFAULT_PORT_NUMBER;
		}

		final Server server = new Server();

		final HttpConfiguration config = new HttpConfiguration();
		config.setSendServerVersion( false );
		final HttpConnectionFactory connectionFactory = new HttpConnectionFactory( config );

		final ServerConnector connector = new ServerConnector( server, connectionFactory );
		connector.setPort( port );
		server.addConnector( connector );
		server.setHandler( new NGJettyHandler( application ) );

		// FIXME: Temporary lifecycle event. Probably removed once we have a stable application initialization cycle // Hugi 2025-10-02
		server.addBean( new LifeCycle.Listener() {
			@Override
			public void lifeCycleStarted( LifeCycle event ) {
				application.adaptorListening();
			}
		} );

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

	public static class NGJettyHandler extends Handler.Abstract {

		private final NGApplication _application;

		public NGJettyHandler( NGApplication application ) {
			_application = application;
		}

		@Override
		public boolean handle( Request request, Response response, Callback callback ) throws Exception {
			return doRequest( request, response, callback );
		}

		private boolean doRequest( final Request jettyRequest, final Response jettyResponse, Callback callback ) throws IOException {

			final NGRequest ngRequest = requestToNGRequest( jettyRequest );

			// Completes the exchange once the response has been written, then lets the request release what it holds (such as the temporary files of an upload)
			final Callback completion = Callback.from( ngRequest::_close, callback );

			// This is where the application logic will perform it's actual work
			final NGResponse ngResponse = _application.dispatchRequest( ngRequest );

			//			FIXME: We need to explicitly enable this to enable "control flow" for the next Jetty Handler // Hugi 2026-02-14
			//			if( ngResponse instanceof NGNotFoundResponse ) {
			//				return false;
			//			}

			jettyResponse.setStatus( ngResponse.status() );

			for( final NGCookie ngCookie : ngResponse.cookies() ) {
				Response.addCookie( jettyResponse, ngCookieToJettyCookie( ngCookie ) );
			}

			for( final Entry<String, List<String>> entry : ngResponse.headers().entrySet() ) {
				jettyResponse.getHeaders().add( entry.getKey(), entry.getValue() );
			}

			switch( ngResponse ) {
				case NGResponseMultipart r -> {
					final ContentSource cs = new MultiPartFormData.ContentSource( NGResponseMultipart.BOUNDARY );

					for( ContentPart part : r._contentParts.values() ) {
						cs.addPart( createStringPart( part.name(), part.content().toString() ) );
					}

					cs.close();

					Content.copy( cs, jettyResponse, completion );
				}
				case NGStreamingResponse r -> {
					final long contentLength = r.contentInputStreamLength();
					final Content.Source cs = Content.Source.from( r.contentInputStream() );

					jettyResponse.getHeaders().put( "content-length", String.valueOf( contentLength ) );
					Content.copy( cs, jettyResponse, completion );
				}
				case NGStandardResponse r -> {
					final long contentLength = r.contentBytesLength();
					final Content.Source cs = Content.Source.from( new ByteArrayInputStream( r.contentBytes() ) );

					jettyResponse.getHeaders().put( "content-length", String.valueOf( contentLength ) );
					Content.copy( cs, jettyResponse, completion );
				}
				default -> {
					// FIXME: THrow a proper exception here. But more importantly — handle responses in an exhaustive way // Hugi 2026-09-21
					throw new RuntimeException( "Unknown response class: " + ngResponse.getClass().getName() );
				}
			}

			return true;
		}

		/**
		 * @return A multipart ContentPart with [name] and [content]
		 */
		private static MultiPart.ContentSourcePart createStringPart( final String name, final String content ) {
			final HttpFields httpFields = HttpFields.build().add( new HttpField( "content-disposition", "form-data; name=\"%s\"".formatted( name ) ) );
			final Source contentSource = Content.Source.from( new ByteArrayInputStream( content.getBytes() ) );
			return new MultiPart.ContentSourcePart( name, null, httpFields, contentSource );
		}

		private static HttpCookie ngCookieToJettyCookie( final NGCookie ngCookie ) {
			final HttpCookie.Builder jettyCookieBuilder = HttpCookie.build( ngCookie.name(), ngCookie.value() );

			if( ngCookie.domain() != null ) {
				jettyCookieBuilder.domain( ngCookie.domain() );
			}

			if( ngCookie.path() != null ) {
				jettyCookieBuilder.path( ngCookie.path() );
			}

			jettyCookieBuilder.httpOnly( ngCookie.httpOnly() );
			jettyCookieBuilder.secure( ngCookie.secure() );

			if( ngCookie.maxAge() != null ) {
				jettyCookieBuilder.maxAge( ngCookie.maxAge() );
			}

			if( ngCookie.sameSite() != null ) {
				jettyCookieBuilder.sameSite( SameSite.from( ngCookie.sameSite().headerValue() ) );
			}

			return jettyCookieBuilder.build();
		}

		/**
		 * @return the given Request converted to an NGRequest. Its body is read, and a multipart body split into parts, when the request asks for it.
		 */
		private static NGRequest requestToNGRequest( final Request jettyRequest ) {
			final String method = jettyRequest.getMethod();
			final String uri = jettyRequest.getHttpURI().getCanonicalPath();
			final String queryString = jettyRequest.getHttpURI().getQuery();
			final String httpVersion = jettyRequest.getConnectionMetaData().getHttpVersion().asString();
			final Map<String, List<String>> headers = headerMap( jettyRequest );
			final InputStream body = Request.asInputStream( jettyRequest );
			final String remoteAddress = Request.getRemoteAddr( jettyRequest );

			return NGRequests.of( method, uri, queryString, httpVersion, headers, body, remoteAddress, NGIncomingParts::new );
		}

		/**
		 * @return The headers from the Request as a Map
		 */
		private static Map<String, List<String>> headerMap( final Request jettyRequest ) {
			final Map<String, List<String>> map = new HashMap<>();

			for( final HttpField httpField : jettyRequest.getHeaders() ) {
				map.put( httpField.getName(), httpField.getValueList() );
			}

			return map;
		}
	}
}