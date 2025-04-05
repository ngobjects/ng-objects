package ng.adaptor.jetty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpCookie.SameSite;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.Fields.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGDevelopmentInstanceStopper;

public class NGAdaptorJetty extends NGAdaptor {

	private static final Logger logger = LoggerFactory.getLogger( NGAdaptorJetty.class );

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

		final Server server = new Server();

		final HttpConfiguration http = new HttpConfiguration();
		final HttpConnectionFactory http11 = new HttpConnectionFactory( http );

		final ServerConnector connector = new ServerConnector( server, http11 );
		connector.setPort( port );
		server.addConnector( connector );

		server.setHandler( new NGHandler() );

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

	public class NGHandler extends Handler.Abstract {

		@Override
		public boolean handle( Request request, Response response, Callback callback ) throws Exception {
			doRequest( request, response, callback );
			return true;
		}

		private void doRequest( final Request jettyRequest, final Response jettyResponse, Callback callback ) throws IOException {

			// This is where the application logic will perform it's actual work
			final NGRequest woRequest = requestToNGRequest( jettyRequest );
			final NGResponse ngResponse = _application.dispatchRequest( woRequest );

			jettyResponse.setStatus( ngResponse.status() );

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

			jettyResponse.getHeaders().add( "content-length", String.valueOf( contentLength ) );

			for( final NGCookie ngCookie : ngResponse.cookies() ) {
				Response.addCookie( jettyResponse, ngCookieToJettyCookie( ngCookie ) );
			}

			for( final Entry<String, List<String>> entry : ngResponse.headers().entrySet() ) {
				for( final String headerValue : entry.getValue() ) {
					jettyResponse.getHeaders().add( entry.getKey(), headerValue );
				}
			}

			try( final OutputStream out = Content.Sink.asOutputStream( jettyResponse )) {
				if( ngResponse.contentInputStream() != null ) {
					try( final InputStream inputStream = ngResponse.contentInputStream()) {
						inputStream.transferTo( out );
					}
				}
				else {
					ngResponse.contentByteStream().writeTo( out );
				}

				// FIXME: I'm doing this to mark the response as completed. Probably not the right way // Hugi 2024-04-05
				Content.Sink.write( jettyResponse, true, "", callback );
			}
			//			if( ngResponse.contentInputStream() != null ) {
			//				jettyResponse.write( isFailed(), null, callback );
			//				try( final InputStream inputStream = ngResponse.contentInputStream()) {
			//					final byte[] bytes = ngResponse.contentInputStream().readAllBytes();
			//					jettyResponse.write( true, ByteBuffer.wrap( bytes ), callback );
			//				}
			//			}
			//			else {
			//				final byte[] bytes = ngResponse.contentByteStream().toByteArray();
			//				jettyResponse.write( true, ByteBuffer.wrap( bytes ), callback );
			//			}
		}

		private static HttpCookie ngCookieToJettyCookie( final NGCookie ngCookie ) {
			final HttpCookie.Builder jettyCookieBuilder = HttpCookie.build( ngCookie.name(), ngCookie.value() );

			if( ngCookie.domain() != null ) {
				jettyCookieBuilder.domain( ngCookie.domain() );
			}

			if( ngCookie.path() != null ) {
				jettyCookieBuilder.path( ngCookie.path() );
			}

			jettyCookieBuilder.httpOnly( ngCookie.isHttpOnly() );
			jettyCookieBuilder.secure( ngCookie.isSecure() );

			if( ngCookie.maxAge() != null ) {
				jettyCookieBuilder.maxAge( ngCookie.maxAge() );
			}

			if( ngCookie.sameSite() != null ) {
				jettyCookieBuilder.sameSite( SameSite.from( ngCookie.sameSite() ) );
			}

			return jettyCookieBuilder.build();
		}

		/**
		 * @return the given HttpServletRequest converted to an NGRequest
		 */
		private static NGRequest requestToNGRequest( final Request sr ) {

			// We read the formValues map before reading the requests content stream, since consuming the content stream will remove POST parameters
			Map<String, List<String>> formValuesFromServletRequest;
			try {
				formValuesFromServletRequest = formValues( Request.getParameters( sr ) );
			}
			catch( Exception e ) {
				throw new RuntimeException( e );
			}

			final ByteArrayOutputStream bos = new ByteArrayOutputStream();

			try( final InputStream is = Request.asInputStream( sr )) {
				is.transferTo( bos );
			}
			catch( final IOException e ) {
				throw new UncheckedIOException( "Failed to consume the HTTP request's inputstream", e );
			}

			// FIXME: Get the protocol
			final NGRequest request = new NGRequest( sr.getMethod(), sr.getHttpURI().getCanonicalPath(), "FIXME", headerMap( sr ), bos.toByteArray() );

			// FIXME: Form value parsing should really happen within the request object, not in the adaptor // Hugi 2021-12-31
			request._setFormValues( formValuesFromServletRequest );

			// FIXME: Cookie parsing should happen within the request object, not in the adaptor // Hugi 2021-12-31
			request._setCookieValues( cookieValues( Request.getCookies( sr ) ) );

			return request;
		}

		/*
		private static void logMultipartRequest( HttpServletRequest sr ) {
			// FIXME: Starting work on multipart request handling. Very much experimental/work in progress // Hugi 2023-04-16
			if( sr.getContentType() != null && sr.getContentType().startsWith( "multipart/form-data" ) ) {
				System.out.println( ">>>>>>>>>> Multipart request detected" );
		
				try {
					// final String string = Files.createTempFile( UUID.randomUUID().toString(), ".fileupload" ).toString();
					// System.out.println( "Multipart temp dir: " + string );
		
					for( Part part : sr.getParts() ) {
						//					MultiPart mp = (MultiPart)part;
						System.out.println( "============= START PART =============" );
						System.out.println( "class: " + part.getClass() );
						System.out.println( "name: " + part.getName() );
						System.out.println( "contentType: " + part.getContentType() );
						System.out.println( "submittedFilename: " + part.getSubmittedFileName() );
						System.out.println( "size: " + part.getSize() );
						System.out.println( "value: " + new String( part.getInputStream().readAllBytes() ) );
		
						System.out.println( "- Headers:" );
						for( String headerName : part.getHeaderNames() ) {
							System.out.println( "-- %s : %s".formatted( headerName, part.getHeaders( headerName ) ) );
		
						}
		
						System.out.println( "============= END PART =============" );
					}
				}
				catch( IOException e ) {
					throw new RuntimeException( e );
				}
			}
		}
		*/

		/**
		 * @return The queryParameters as a formValue Map (our format)
		 */
		private static Map<String, List<String>> formValues( final Fields queryParameters ) {

			Map<String, List<String>> map = new HashMap<>();

			for( Field entry : queryParameters ) {
				map.put( entry.getName(), entry.getValues() );
			}

			return map;
		}

		/**
		 * @return The listed cookies as a map
		 */
		private static Map<String, List<String>> cookieValues( final List<HttpCookie> cookies ) {
			final Map<String, List<String>> cookieValues = new HashMap<>();

			if( cookies != null ) {
				for( HttpCookie cookie : cookies ) {
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
		private static Map<String, List<String>> headerMap( final Request servletRequest ) {
			final Map<String, List<String>> map = new HashMap<>();

			final HttpFields headerNamesEnumeration = servletRequest.getHeaders();

			for( final HttpField httpField : headerNamesEnumeration ) {
				map.put( httpField.getName(), httpField.getValueList() );
			}

			return map;
		}
	}
}