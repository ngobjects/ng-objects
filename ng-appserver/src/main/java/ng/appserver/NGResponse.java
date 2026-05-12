package ng.appserver;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * FIXME: Need to decide what to do about responses of different types.
 * Is a string response the same type as a binary response or a streaming response, or should these have different implementations
 * Are responses even mutable? A mutable response should possibly have a different design or a builder
 * // Hugi 2022-06-05
 */

public class NGResponse implements NGMessageInterface, NGActionResults {

	/**
	 * The Response's status code. Defaults to 200.
	 */
	private int _status = 200;

	/**
	 * Cookies set by the response.
	 */
	private List<NGCookie> _cookies = new ArrayList<>();

	/**
	 * Data to be streamed to the client
	 */
	private InputStream _contentInputStream;

	/**
	 * Length of the stream to be streamed to the client
	 *
	 * The initial value is set to -1, meaning no content length has been set.
	 * We will check if the value has been set when returning the response, to ensure it's so.
	 */
	private long _contentInputStreamLength = -1;

	/**
	 * Creates an empty NGResponse with status 200
	 */
	@Deprecated
	public NGResponse() {}

	@Deprecated
	public NGResponse( final byte[] bytes, final int status ) {
		setContentBytes( bytes );
		setStatus( status );
	}

	@Deprecated
	public NGResponse( final String contentString, final int status ) {
		setContentString( contentString );
		setStatus( status );
	}

	public int status() {
		return _status;
	}

	public void setStatus( final int status ) {
		_status = status;
	}

	/**
	 * @return A list of HTTP cookies that this response will set (i.e. create a set-cookie header for)
	 */
	public List<NGCookie> cookies() {
		return _cookies;
	}

	/**
	 * Add the given cookie to the response.
	 */
	public void addCookie( final NGCookie cookie ) {
		Objects.requireNonNull( cookie );
		_cookies.add( cookie );
	}

	public void setContentInputStream( final InputStream inputStream, final long contentInputStreamLength ) {
		_contentInputStream = inputStream;
		_contentInputStreamLength = contentInputStreamLength;
	}

	public InputStream contentInputStream() {
		return _contentInputStream;
	}

	public long contentInputStreamLength() {
		return _contentInputStreamLength;
	}

	@Override
	public NGResponse generateResponse() {
		return this;
	}

	/* ------ FIXME: below is logic from NGMessage, awaiting to be nicely factored into the class // Hugi 2026-05-12 ------  */

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