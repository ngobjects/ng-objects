package ng.adaptor.jetty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGServletAdaptor extends HttpServlet {

	private NGApplication _application;

	public NGServletAdaptor( NGApplication application ) {
		Objects.requireNonNull( application );
		_application = application;
	}

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
		final NGResponse ngResponse = _application.dispatchRequest( woRequest );

		servletResponse.setStatus( ngResponse.status() );

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

		servletResponse.setHeader( "content-length", String.valueOf( contentLength ) );

		for( final NGCookie ngCookie : ngResponse.cookies() ) {
			servletResponse.addCookie( ngCookieToServletCookie( ngCookie ) );
		}

		for( final Entry<String, List<String>> entry : ngResponse.headers().entrySet() ) {
			for( final String headerValue : entry.getValue() ) {
				servletResponse.addHeader( entry.getKey(), headerValue );
			}
		}

		try( final OutputStream out = servletResponse.getOutputStream()) {
			if( ngResponse.contentInputStream() != null ) {
				try( final InputStream inputStream = ngResponse.contentInputStream()) {
					inputStream.transferTo( out );
				}
			}
			else {
				ngResponse.contentByteStream().writeTo( out );
			}
		}
	}

	private static Cookie ngCookieToServletCookie( final NGCookie ngCookie ) {
		final Cookie servletCookie = new Cookie( ngCookie.name(), ngCookie.value() );

		servletCookie.setVersion( 1 );

		if( ngCookie.domain() != null ) {
			servletCookie.setDomain( ngCookie.domain() );
		}

		if( ngCookie.path() != null ) {
			servletCookie.setPath( ngCookie.path() );
		}

		servletCookie.setHttpOnly( ngCookie.isHttpOnly() );
		servletCookie.setSecure( ngCookie.isSecure() );

		if( ngCookie.maxAge() != null ) {
			servletCookie.setMaxAge( ngCookie.maxAge() );
		}

		if( ngCookie.sameSite() != null ) {
			servletCookie.setAttribute( "SameSite", ngCookie.sameSite() );
		}

		return servletCookie;
	}

	/**
	 * @return the given HttpServletRequest converted to an NGRequest
	 */
	private static NGRequest servletRequestToNGRequest( final HttpServletRequest sr ) {

		// FIXME: Starting work on multipart request handling. Very much experimental/work in progress // Hugi 2023-04-16
		if( sr.getContentType() != null && sr.getContentType().startsWith( "multipart/form-data" ) ) {
			System.out.println( ">>>>>>>>>> Multipart request detected" );

			try {
				final String string = Files.createTempFile( UUID.randomUUID().toString(), ".fileupload" ).toString();
				System.out.println( "Multipart temp dir: " + string );
				//				sr.setAttribute( Request.__MULTIPART_CONFIG_ELEMENT, new MultipartConfigElement( string ) );

				for( Part part : sr.getParts() ) {
					//					MultiPart mp = (MultiPart)part;
					System.out.println( "============= START PARTS =============" );
					System.out.println( part.getClass() );
					System.out.println( part.getContentType() );
					System.out.println( part.getName() );
					System.out.println( part.getSubmittedFileName() );
					System.out.println( part.getSize() );

					System.out.println( "- Headers:" );
					for( String headerName : part.getHeaderNames() ) {
						System.out.println( "-- %s : %s".formatted( headerName, part.getHeaders( headerName ) ) );

					}

					System.out.println( "============= END PARTS =============" );
				}
			}
			catch( IOException | ServletException e ) {
				throw new RuntimeException( e );
			}
		}

		// We read the formValues map before reading the requests content stream, since consuming the content stream will remove POST parameters
		final Map<String, List<String>> formValuesFromServletRequest = formValues( sr.getParameterMap() );

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try( final InputStream is = sr.getInputStream()) {
			is.transferTo( bos );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( "Failed to consume the HTTP request's inputstream", e );
		}

		final NGRequest request = new NGRequest( sr.getMethod(), sr.getRequestURI(), sr.getProtocol(), headerMap( sr ), bos.toByteArray() );

		// FIXME: Form value parsing should really happen within the request object, not in the adaptor // Hugi 2021-12-31
		request._setFormValues( formValuesFromServletRequest );

		// FIXME: Cookie parsing should happen within the request object, not in the adaptor // Hugi 2021-12-31
		request._setCookieValues( cookieValues( sr.getCookies() ) );

		return request;
	}

	/**
	 * @return The queryParameters as a formValue Map (our format)
	 */
	private static Map<String, List<String>> formValues( Map<String, String[]> queryParameters ) {

		Map<String, List<String>> map = new HashMap<>();

		for( Entry<String, String[]> entry : queryParameters.entrySet() ) {
			map.put( entry.getKey(), Arrays.asList( entry.getValue() ) );
		}

		return map;
	}

	/**
	 * @return The listed cookies as a map
	 */
	private static Map<String, List<String>> cookieValues( final Cookie[] cookies ) {
		final Map<String, List<String>> cookieValues = new HashMap<>();

		if( cookies != null ) {
			for( Cookie cookie : cookies ) {
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
	private static Map<String, List<String>> headerMap( final HttpServletRequest servletRequest ) {
		final Map<String, List<String>> map = new HashMap<>();

		final Enumeration<String> headerNamesEnumeration = servletRequest.getHeaderNames();

		while( headerNamesEnumeration.hasMoreElements() ) {
			final String headerName = headerNamesEnumeration.nextElement();
			final List<String> values = new ArrayList<>();
			map.put( headerName, values );

			final Enumeration<String> headerValuesEnumeration = servletRequest.getHeaders( headerName );

			while( headerValuesEnumeration.hasMoreElements() ) {
				values.add( headerValuesEnumeration.nextElement() );
			}
		}

		return map;
	}
}