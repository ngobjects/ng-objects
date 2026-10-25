package ng.adaptor.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ng.appserver.NGApplication;
import ng.appserver.http.NGCookie;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGRequests;
import ng.appserver.http.NGResponse;
import ng.appserver.http.NGStandardRequest;
import ng.appserver.http.NGStandardResponse;
import ng.appserver.http.NGStreamingResponse;

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

		final NGStandardRequest ngRequest = servletRequestToNGRequest( servletRequest );

		try {
			// This is where the application logic will perform it's actual work
			final NGResponse ngResponse = _application.dispatchRequest( ngRequest );
			writeResponse( ngResponse, servletResponse );
		}
		finally {
			// Lets the request release what it holds, such as the temporary files of an upload
			ngRequest._close();
		}
	}

	private static void writeResponse( final NGResponse ngResponse, final HttpServletResponse servletResponse ) throws IOException {

		servletResponse.setStatus( ngResponse.status() );

		for( final NGCookie ngCookie : ngResponse.cookies() ) {
			servletResponse.addCookie( ngCookieToServletCookie( ngCookie ) );
		}

		for( final Entry<String, List<String>> entry : ngResponse.headers().entrySet() ) {
			for( final String headerValue : entry.getValue() ) {
				servletResponse.addHeader( entry.getKey(), headerValue );
			}
		}

		switch( ngResponse ) {
			case NGStreamingResponse r -> {
				servletResponse.setContentLengthLong( r.contentInputStreamLength() );

				try( final OutputStream out = servletResponse.getOutputStream(); final InputStream inputStream = r.contentInputStream()) {
					inputStream.transferTo( out );
				}
			}
			case NGStandardResponse r -> {
				servletResponse.setContentLengthLong( r.contentBytesLength() );

				try( final OutputStream out = servletResponse.getOutputStream()) {
					r.contentByteStream().writeTo( out );
				}
			}
			default -> {
				// FIXME: Handle responses in an exhaustive way, see NGAdaptorJetty // Hugi 2026-09-21
				throw new RuntimeException( "Unknown response class: " + ngResponse.getClass().getName() );
			}
		}
	}

	private static Cookie ngCookieToServletCookie( final NGCookie ngCookie ) {
		final Cookie servletCookie = new Cookie( ngCookie.name(), ngCookie.value() );

		if( ngCookie.domain() != null ) {
			servletCookie.setDomain( ngCookie.domain() );
		}

		if( ngCookie.path() != null ) {
			servletCookie.setPath( ngCookie.path() );
		}

		servletCookie.setHttpOnly( ngCookie.httpOnly() );
		servletCookie.setSecure( ngCookie.secure() );

		if( ngCookie.maxAge() != null ) {
			final int intMaxAge = (int)Math.min( ngCookie.maxAge(), Integer.MAX_VALUE ); // We have to cap the maxAge since the servlet API uses an int for the value
			servletCookie.setMaxAge( intMaxAge );
		}

		if( ngCookie.sameSite() != null ) {
			servletCookie.setAttribute( "SameSite", ngCookie.sameSite().headerValue() );
		}

		return servletCookie;
	}

	/**
	 * @return the given HttpServletRequest converted to an NGRequest
	 */
	private static NGStandardRequest servletRequestToNGRequest( final HttpServletRequest sr ) {
		try {
			return NGRequests.of( sr.getMethod(), sr.getRequestURI(), sr.getQueryString(), sr.getProtocol(), headerMap( sr ), cookieValues( sr.getCookies() ), sr.getInputStream(), sr.getRemoteAddr(), NGIncomingParts::new );
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
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