package ng.appserver;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * FIXME:
 * Need to decide what to do about responses of different types.
 * Is a string response the same type as a binary response or a streaming response, or should these have different implementations
 * Are responses even mutable? A mutable response should possibly have a different design or a builder
 * // Hugi 2022-06-05
 */

public class NGResponse extends NGMessage implements NGActionResults {

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
	 */
	private long _contentInputStreamLength;

	/**
	 * Creates an empty NGResponse with status 200
	 */
	public NGResponse() {}

	public NGResponse( final byte[] bytes, final int status ) {
		setContentBytes( bytes );
		setStatus( status );
	}

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
}