package ng.adaptor.jetty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpCookie.SameSite;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MultiPartConfig;
import org.eclipse.jetty.http.MultiPartFormData;
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
import org.eclipse.jetty.util.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGRequest.UploadedFile;
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

			final String contentType = jettyRequest.getHeaders().get( HttpHeader.CONTENT_TYPE );

			final NGRequest ngRequest;

			if( contentType != null && contentType.contains( "multipart/form-data" ) ) {
				ngRequest = multipartRequestToNGRequest( jettyRequest, contentType, callback );
			}
			else {
				ngRequest = requestToNGRequest( jettyRequest );
			}

			// This is where the application logic will perform it's actual work
			final NGResponse ngResponse = _application.dispatchRequest( ngRequest );

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
			}

			callback.succeeded();
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
		 * @return the given Request converted to an NGRequest
		 */
		private static NGRequest multipartRequestToNGRequest( final Request jettyRequest, final String contentType, final Callback callback ) {

			final MultiPartConfig config = new MultiPartConfig.Builder()
					.location( Path.of( "/tmp/jet" ) )
					.build();

			// The formValues that will get set on the request
			final Map<String, List<String>> formValues = new HashMap<>();

			// The uploaded files, if any
			final Map<String, UploadedFile> uploadedFiles = new HashMap<>();

			MultiPartFormData.onParts( jettyRequest, jettyRequest, contentType, config, new Promise.Invocable<MultiPartFormData.Parts>() {

				@Override
				public void succeeded( MultiPartFormData.Parts parts ) {
					parts.forEach( p -> {
						final String partContentType = p.getHeaders().get( HttpHeader.CONTENT_TYPE );

						final String parameterName = p.getName();
						final String parameterValue;

						// We're assuming that if this part does not have a content type, it's a regular ol' form value, to be added to the requests formValues map as usual.
						if( partContentType == null ) {
							parameterValue = p.getContentAsString( StandardCharsets.UTF_8 ); // FIXME: Hardcoding the character set is a little presumptuous // Hugi 2025-04-05
						}
						else {
							// We'll add the filename as the parameter's value here. That can then be used to fetch the uploaded data in the request's uploadedFiles map
							parameterValue = p.getFileName();

							// Now we add the uploaded file to the request
							final UploadedFile file = new UploadedFile( p.getFileName(), partContentType, Content.Source.asInputStream( p.getContentSource() ), p.getLength() );
							uploadedFiles.put( p.getFileName(), file );
						}

						List<String> list = formValues.get( parameterName );

						if( list == null ) {
							list = new ArrayList<>();
							formValues.put( p.getName(), list );
						}

						list.add( parameterValue );
					} );
				}

				@Override
				public void failed( Throwable failure ) {
					throw new RuntimeException( failure );
				}

				@Override
				public InvocationType getInvocationType() {
					return InvocationType.NON_BLOCKING;
				}
			} );

			final NGRequest request = new NGRequest( jettyRequest.getMethod(), jettyRequest.getHttpURI().getCanonicalPath(), "FIXME", headerMap( jettyRequest ), new byte[] {} ); // FIXME: It makes little sense to set the request content to be empty here... // Hugi 2025-04-05
			request._setFormValues( formValues );
			request._setCookieValues( cookieValues( Request.getCookies( jettyRequest ) ) );
			uploadedFiles.entrySet().forEach( p -> request._uploadedFiles().put( p.getKey(), p.getValue() ) ); // FIXME: Adding uploaded files this way is really, really temporary // Hugi 2025-04-05
			return request;
		}

		/**
		 * @return the given Request converted to an NGRequest
		 */
		private static NGRequest requestToNGRequest( final Request jettyRequest ) {

			// We read the formValues map before reading the requests content stream, since consuming the content stream will remove POST parameters
			final Map<String, List<String>> formValues = formValuesFromRequest( jettyRequest );

			final ByteArrayOutputStream bos = new ByteArrayOutputStream();

			try( final InputStream is = Request.asInputStream( jettyRequest )) {
				is.transferTo( bos );
			}
			catch( final IOException e ) {
				throw new UncheckedIOException( "Failed to consume the HTTP request's inputstream", e );
			}

			// FIXME: Get the protocol
			final NGRequest request = new NGRequest( jettyRequest.getMethod(), jettyRequest.getHttpURI().getCanonicalPath(), "FIXME", headerMap( jettyRequest ), bos.toByteArray() );

			// FIXME: Form value parsing should really happen within the request object, not in the adaptor // Hugi 2021-12-31
			request._setFormValues( formValues );

			// FIXME: Cookie parsing should happen within the request object, not in the adaptor // Hugi 2021-12-31
			request._setCookieValues( cookieValues( Request.getCookies( jettyRequest ) ) );

			return request;
		}

		/**
		 * @return The queryParameters as a formValue Map (our format)
		 */
		private static Map<String, List<String>> formValuesFromRequest( final Request jettyRequest ) {

			Fields parameters;

			try {
				parameters = Request.getParameters( jettyRequest );
			}
			catch( Exception e ) {
				throw new RuntimeException( e );
			}

			final Map<String, List<String>> map = new HashMap<>();

			for( Field entry : parameters ) {
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