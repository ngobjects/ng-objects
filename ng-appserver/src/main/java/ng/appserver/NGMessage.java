package ng.appserver;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Parent class of NGResponse/NGRequest
 */

public abstract class NGMessage implements NGMessageInterface {

	/**
	 * The headers  of this message
	 */
	private Map<String, List<String>> _headers = NGMessageInterface.createEmptyHeadersMap();

	/**
	 * The content of this message
	 *
	 * FIXME:
	 * Currently this stores all types of content. We're going to want to use more efficient types for different response types (string/data/streaming)
	 * For example, it's clear that using a StringBuilder for string responses is significantly more efficient than using the ByteArrayOutputStream
	 * // Hugi 2023-02-08
	 */
	private ByteArrayOutputStream _contentByteStream = new ByteArrayOutputStream( NGMessageInterface.DEFAULT_CONTENT_DATA_LENGTH );

	/**
	 * @return The HTTP headers of this message
	 */
	@Override
	public Map<String, List<String>> headers() {
		return _headers;
	}

	/**
	 * Sets the headers from the given map.
	 */
	@Override
	public void setHeaders( final Map<String, List<String>> newHeaders ) {
		_headers = NGMessageInterface.createEmptyHeadersMap();

		for( Entry<String, List<String>> header : newHeaders.entrySet() ) {
			_headers.put( header.getKey(), header.getValue() );
		}
	}

	/**
	 * @return The response's content stream
	 */
	@Override
	public ByteArrayOutputStream contentByteStream() {
		return _contentByteStream;
	}

	@Override
	public void _setContentByteStream( ByteArrayOutputStream value ) {
		_contentByteStream = value;
	}
}