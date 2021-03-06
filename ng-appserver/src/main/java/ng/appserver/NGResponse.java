package ng.appserver;

import java.util.ArrayList;
import java.util.List;

/**
 * FIXME: Need to decide what to do about responses of different types. // Hugi 2022-06-05
 * Is a string response the same type as a binary response or a streaming response, or should these have different implementations
 * Are responses even mutable? A mutable response should possibly have a different design or a builder
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
		_cookies.add( cookie );
	}

	@Override
	public NGResponse generateResponse() {
		return this;
	}
}