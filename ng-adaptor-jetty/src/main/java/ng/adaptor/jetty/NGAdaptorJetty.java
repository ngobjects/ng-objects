package ng.adaptor.jetty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpCookie.SameSite;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.http.MultiPartConfig;
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
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.Fields.Field;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAdaptor;
import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGRequest.UploadedFile;
import ng.appserver.NGResponse;
import ng.appserver.NGResponseMultipart;
import ng.appserver.NGResponseMultipart.ContentPart;
import ng.appserver.privates.NGDevelopmentInstanceStopper;

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
			doRequest( request, response, callback );
			return true;
		}

		private void doRequest( final Request jettyRequest, final Response jettyResponse, Callback callback ) throws IOException {

			final String contentType = jettyRequest.getHeaders().get( HttpHeader.CONTENT_TYPE );

			final NGRequest ngRequest;

			if( contentType != null && contentType.contains( "multipart/form-data" ) ) {
				ngRequest = multipartRequestToNGRequest( jettyRequest, contentType );
			}
			else {
				ngRequest = requestToNGRequest( jettyRequest );
			}

			// This is where the application logic will perform it's actual work
			final NGResponse ngResponse = _application.dispatchRequest( ngRequest );

			jettyResponse.setStatus( ngResponse.status() );

			for( final NGCookie ngCookie : ngResponse.cookies() ) {
				Response.addCookie( jettyResponse, ngCookieToJettyCookie( ngCookie ) );
			}

			for( final Entry<String, List<String>> entry : ngResponse.headers().entrySet() ) {
				jettyResponse.getHeaders().add( entry.getKey(), entry.getValue() );
			}

			if( ngResponse instanceof NGResponseMultipart mp ) {
				final ContentSource cs = new MultiPartFormData.ContentSource( NGResponseMultipart.BOUNDARY );

				for( ContentPart part : mp._contentParts.values() ) {
					cs.addPart( createStringPart( part.name(), part.content().toString() ) );
				}

				cs.close();

				Content.copy( cs, jettyResponse, callback );
			}
			else {
				final Content.Source cs;
				final long contentLength;

				if( ngResponse.contentInputStream() != null ) {
					contentLength = ngResponse.contentInputStreamLength();

					if( contentLength == -1 ) {
						throw new IllegalArgumentException( "NGResponse.contentInputStream() is set but contentInputLength has not been set. You must provide the content length when serving an InputStream" );
					}

					cs = Content.Source.from( ngResponse.contentInputStream() );
				}
				else {
					contentLength = ngResponse.contentBytesLength();
					cs = Content.Source.from( new ByteArrayInputStream( ngResponse.contentBytes() ) );
				}

				jettyResponse.getHeaders().put( "content-length", String.valueOf( contentLength ) );
				Content.copy( cs, jettyResponse, callback );
			}
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
		 * @return Jetty's configuration for handling of multipart request parsing
		 *
		 * FIXME: MultiPart configuration needs to be configurable on ng's side. Or we need to expose the Jetty configuration (which I'd prefer not to, since that makes configuration implementation dependent) // Hugi 2025-06-17
		 */
		private static MultiPartConfig multiPartConfig() {
			return new MultiPartConfig.Builder()
					.location( Path.of( "/tmp/ngmultijet" ) ) // Path to a directory used by Jetty to store cached data during processing of multipart requests
					.build();
		}

		/**
		 * @return the given Request converted to an NGRequest
		 */
		private static NGRequest multipartRequestToNGRequest( final Request jettyRequest, final String contentType ) {

			// Start by obtaining the regular formValues from the request (for example, query parameters)
			final Map<String, List<String>> formValues = parametersFromRequest( jettyRequest );

			// Uploaded files to set on the request
			final Map<String, UploadedFile> uploadedFiles = new HashMap<>();

			MultiPartFormData
					.getParts( jettyRequest, jettyRequest, contentType, multiPartConfig() )
					.forEach( part -> {
						final String partContentType = part.getHeaders().get( HttpHeader.CONTENT_TYPE );

						final String parameterValue;

						if( partContentType == null ) {
							// If this part does not have a content type, we treat it like a regular ol' form value, added to the request's formValues map as usual
							parameterValue = part.getContentAsString( StandardCharsets.UTF_8 ); // FIXME: Hardcoding the character set is a little presumptuous // Hugi 2025-04-05
						}
						else {
							// We generate a unique ID to store the attachment under. The ID will be stored as a value under the part's name in the request's formValues, where it can be used to obtaing the uploaded file from the request's uploadedFiles map
							parameterValue = UUID.randomUUID().toString();

							// Now we add the uploaded file to the request
							final UploadedFile file = new UploadedFile( part.getFileName(), partContentType, Content.Source.asInputStream( part.getContentSource() ), part.getLength() );
							uploadedFiles.put( parameterValue, file );
						}

						// Finally, we add our "value" to the request's form values
						formValues
								.computeIfAbsent( part.getName(), _unused -> new ArrayList<>() )
								.add( parameterValue );
					} );

			final String method = jettyRequest.getMethod();
			final String uri = jettyRequest.getHttpURI().getCanonicalPath();
			final String httpVersion = jettyRequest.getConnectionMetaData().getHttpVersion().asString();
			final Map<String, List<String>> headers = headerMap( jettyRequest );
			final Map<String, List<String>> cookieValues = cookieValues( Request.getCookies( jettyRequest ) );
			final byte[] content = new byte[] {}; // FIXME: This just kind of reflects the badness of our request data model. The request's "content" is really the part list ... // Hugi 2025-04-05

			final NGRequest request = new NGRequest( method, uri, httpVersion, headers, formValues, cookieValues, content );
			uploadedFiles.entrySet().forEach( p -> request._uploadedFiles().put( p.getKey(), p.getValue() ) ); // FIXME: Adding uploaded files this way is really, really temporary // Hugi 2025-04-05
			return request;
		}

		/**
		 * @return the given Request converted to an NGRequest
		 */
		private static NGRequest requestToNGRequest( final Request jettyRequest ) {

			// We read the formValues map before reading the requests content stream, since consuming the content stream will remove POST parameters
			final Map<String, List<String>> formValues = parametersFromRequest( jettyRequest );

			final ByteArrayOutputStream bos = new ByteArrayOutputStream();

			// FIXME: we're always consuming the request's entire body at this point. Allowing us to pass in a stream would be... sensible // Hugi 2025-06-09
			try( final InputStream is = Request.asInputStream( jettyRequest )) {
				is.transferTo( bos );
			}
			catch( final IOException e ) {
				throw new UncheckedIOException( "Failed to consume the HTTP request's inputstream", e );
			}

			final String method = jettyRequest.getMethod();
			final String uri = jettyRequest.getHttpURI().getCanonicalPath();
			final String httpVersion = jettyRequest.getConnectionMetaData().getHttpVersion().asString();
			final Map<String, List<String>> headers = headerMap( jettyRequest );
			final Map<String, List<String>> cookieValues = cookieValues( Request.getCookies( jettyRequest ) );
			final byte[] content = bos.toByteArray();

			return new NGRequest( method, uri, httpVersion, headers, formValues, cookieValues, content );
		}

		/**
		 * @return The queryParameters as a formValue Map (our format)
		 */
		private static Map<String, List<String>> parametersFromRequest( final Request jettyRequest ) {

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