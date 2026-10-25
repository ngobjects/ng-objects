package ng.appserver.http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import ng.appserver.NGApplication;

/**
 * Creates requests from what the adaptor received
 */

public class NGRequests {

	private NGRequests() {}

	/**
	 * @param queryString The URI's query string, still percent-encoded. null if the URI has none.
	 * @param body The request's body. Readable only while the adaptor is handling the request.
	 * @param multipartParser Splits a multipart body into its parts. null if the adaptor doesn't provide one, in which case a multipart request's body is left as it is.
	 * @return A request for what the adaptor received: an NGMultipartRequest if its content type is multipart/form-data. Invoke _close() on it once its response has been sent.
	 */
	public static NGRequest of( final String method, final String uri, final String queryString, final String httpVersion, final Map<String, List<String>> headers, final InputStream body, final String remoteAddress, final NGMultipartParser multipartParser ) {
		final Map<String, List<String>> caseInsensitiveHeaders = NGHeaderMaps.create();
		caseInsensitiveHeaders.putAll( headers );

		final List<String> contentTypes = caseInsensitiveHeaders.get( "content-type" );
		final String contentType = contentTypes == null || contentTypes.isEmpty() ? null : contentTypes.get( 0 );

		final NGRequestLimits limits = NGApplication.application() == null ? NGRequestLimits.DEFAULT : NGRequestLimits.from( NGApplication.application().properties() );

		if( multipartParser != null && contentType != null && "multipart/form-data".equalsIgnoreCase( NGRequestHead.mediaType( contentType ) ) ) {
			final String boundary = NGRequestHead.parameter( contentType, "boundary" );

			if( boundary != null && !boundary.isEmpty() ) {
				return new NGMultipartRequest( method, uri, queryString, httpVersion, headers, body, boundary, multipartParser, limits, remoteAddress );
			}
		}

		return new NGStandardRequest( method, uri, queryString, httpVersion, headers, body, remoteAddress, limits );
	}
}
