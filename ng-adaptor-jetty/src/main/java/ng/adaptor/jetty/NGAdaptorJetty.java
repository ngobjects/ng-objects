package ng.adaptor.jetty;

import java.io.ByteArrayInputStream;
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
import org.eclipse.jetty.util.Promise;
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

		final HttpConfiguration config = new HttpConfiguration();
		final HttpConnectionFactory connectionFactory = new HttpConnectionFactory( config );

		final ServerConnector connector = new ServerConnector( server, connectionFactory );
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

		/**
		 * The directory used by Jetty to store cached data during processing of multipart requests
		 *
		 * FIXME: This should probably be specified in a property // Hugi 2025-06-17
		 */
		private static final String multipartTemporaryDirectory() {
			return "/tmp/ngmultijet";
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
				ngRequest = multipartRequestToNGRequest( jettyRequest, contentType, callback );
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
				for( final String headerValue : entry.getValue() ) {
					jettyResponse.getHeaders().add( entry.getKey(), headerValue );
				}
			}

			if( ngResponse instanceof NGResponseMultipart mp ) {
				final ContentSource cs = new MultiPartFormData.ContentSource( NGResponseMultipart.BOUNDARY );

				for( ContentPart part : mp._contentParts.values() ) {
					cs.addPart( createStringPart( part.name(), part.content().toString() ) );
				}

				cs.close();

				Content.copy( cs, jettyResponse, callback );
			}
			else if( ngResponse.contentInputStream() != null ) {
				final long contentLength = ngResponse.contentInputStreamLength(); // If an InputStream is present, the stream's length must be present as well

				if( contentLength == -1 ) {
					throw new IllegalArgumentException( "NGResponse.contentInputStream() is set but contentInputLength has not been set. You must provide the content length when serving an InputStream" );
				}

				jettyResponse.getHeaders().put( "content-length", String.valueOf( contentLength ) );

				//	try( final InputStream inputStream = ngResponse.contentInputStream()) {
				//		inputStream.transferTo( out );
				//	}
				//
				//	callback.succeeded();
				//
				// FIXME:
				// Above is the old way we served streaming resources. Below is what's probably the proper way to serve streams.
				// Keeping the old version around for some reviewing/performance/correctness testing.
				// Mostly becomes relevant once we're generally serving resources using streams (which will require a little more work
				// on resource management in general, especially having resources keep track of their length).
				// Also; double check that ng's InputStream is getting closed by Jetty once copied, otherwise keep the auto-closing from above.
				// Hugi 2025-06-17

				final Content.Source cs = Content.Source.from( ngResponse.contentInputStream() );
				Content.copy( cs, jettyResponse, callback );
			}
			else {
				try( final OutputStream out = Content.Sink.asOutputStream( jettyResponse )) {
					final long contentLength = ngResponse.contentBytesLength();
					jettyResponse.getHeaders().put( "content-length", String.valueOf( contentLength ) );
					ngResponse.contentByteStream().writeTo( out );
					callback.succeeded();
				}
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
		 * @return the given Request converted to an NGRequest
		 */
		private static NGRequest multipartRequestToNGRequest( final Request jettyRequest, final String contentType, final Callback callback ) {

			final MultiPartConfig config = new MultiPartConfig.Builder()
					.location( Path.of( multipartTemporaryDirectory() ) )
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
							// We're generating a unique ID here to store the attachment under in the request. This value will be stored in the request's formValues, and can be used to fetch the uploaded data in the request's uploadedFiles map
							final String uniqueID = UUID.randomUUID().toString();

							parameterValue = uniqueID;

							// Now we add the uploaded file to the request
							final UploadedFile file = new UploadedFile( p.getFileName(), partContentType, Content.Source.asInputStream( p.getContentSource() ), p.getLength() );
							uploadedFiles.put( uniqueID, file );
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
					// FIXME: This is here temporarily for some debugging. We should have better error handling overall for multipart uploads // Hugi 2025-07-19
					System.out.println( "Multipart fail?" );
					// throw new RuntimeException( failure );
				}
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
			final Map<String, List<String>> formValues = formValuesFromRequest( jettyRequest );

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