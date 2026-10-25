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
	 * @return A request of the class its content type calls for: an NGMultipartRequest for multipart/form-data, an NGStandardRequest otherwise. Invoke _close() on it once its response has been sent.
	 */
	public static NGStandardRequest of( final String method, final String uri, final String queryString, final String httpVersion, final Map<String, List<String>> headers, final InputStream body, final String remoteAddress, final NGMultipartParser multipartParser ) {
		final Map<String, List<String>> caseInsensitiveHeaders = NGMessage.createEmptyHeadersMap();
		caseInsensitiveHeaders.putAll( headers );

		final List<String> contentTypes = caseInsensitiveHeaders.get( "content-type" );
		final String contentType = contentTypes == null || contentTypes.isEmpty() ? null : contentTypes.get( 0 );

		if( multipartParser != null && contentType != null && "multipart/form-data".equalsIgnoreCase( NGStandardRequest.mediaType( contentType ) ) ) {
			final String boundary = NGStandardRequest.parameter( contentType, "boundary" );

			if( boundary != null && !boundary.isEmpty() ) {
				final NGMultipartRequest.Limits limits = NGApplication.application() == null ? NGMultipartRequest.Limits.DEFAULT : NGMultipartRequest.Limits.from( NGApplication.application().properties() );
				return new NGMultipartRequest( method, uri, queryString, httpVersion, headers, body, boundary, multipartParser, limits, remoteAddress );
			}
		}

		return new NGStandardRequest( method, uri, queryString, httpVersion, headers, body, remoteAddress );
	}
}
