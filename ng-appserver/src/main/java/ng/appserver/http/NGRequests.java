package ng.appserver.http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ng.appserver.NGApplication;
import ng.appserver.properties.NGProperties;

/**
 * Creates requests from what the adaptor received
 */

public class NGRequests {

	/**
	 * The limits read from the properties of the application they were read for, kept since properties don't change while an application runs
	 */
	private record CachedLimits( NGProperties properties, NGRequestLimits limits ) {}

	private static volatile CachedLimits _cachedLimits;

	private NGRequests() {}

	/**
	 * @return The request limits set by the running application's properties, DEFAULT if no application is running
	 */
	private static NGRequestLimits limits() {
		final NGApplication application = NGApplication.application();

		if( application == null ) {
			return NGRequestLimits.DEFAULT;
		}

		final CachedLimits cached = _cachedLimits;

		if( cached != null && cached.properties() == application.properties() ) {
			return cached.limits();
		}

		final NGRequestLimits limits = NGRequestLimits.from( application.properties() );
		_cachedLimits = new CachedLimits( application.properties(), limits );
		return limits;
	}

	/**
	 * @param queryString The URI's query string, still percent-encoded. null if the URI has none.
	 * @param body The request's body. Readable only while the adaptor is handling the request.
	 * @param multipartParser Splits a multipart body into its parts. null if the adaptor doesn't provide one, in which case a multipart request's body is left as it is.
	 * @return A request for what the adaptor received: an NGMultipartRequest if its content type is multipart/form-data. Invoke _close() on it once its response has been sent.
	 */
	public static NGRequest of( final String method, final String uri, final String queryString, final Map<String, List<String>> headers, final InputStream body, final String remoteAddress, final NGMultipartParser multipartParser ) {
		final Map<String, List<String>> caseInsensitiveHeaders = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
		caseInsensitiveHeaders.putAll( headers );

		final List<String> contentTypes = caseInsensitiveHeaders.get( "content-type" );
		final String contentType = contentTypes == null || contentTypes.isEmpty() ? null : contentTypes.get( 0 );

		final NGRequestLimits limits = limits();

		if( multipartParser != null && contentType != null && "multipart/form-data".equalsIgnoreCase( NGRequestHead.mediaType( contentType ) ) ) {
			final String boundary = NGRequestHead.parameter( contentType, "boundary" );

			if( boundary != null && !boundary.isEmpty() ) {
				return new NGMultipartRequest( method, uri, queryString, headers, body, boundary, multipartParser, limits, remoteAddress );
			}
		}

		return new NGStandardRequest( method, uri, queryString, headers, body, remoteAddress, limits );
	}
}
