package ng.appserver;

import java.util.ArrayList;
import java.util.List;

/**
 * FIXME:
 * Need to decide what to do about responses of different types.
 * Is a string response the same type as a binary response or a streaming response, even?
 *
 * Are responses even mutable? I think not. It's mutable right now, but a mutable response should possibly be a different structure or a builder
 */

public class NGResponse extends NGMessage implements NGActionResults {

	/**
	 * FIXME: Decide if we want a default
	 */
	private int _status;

	private List<NGCookie> _cookies = new ArrayList<>();

	public NGResponse() {
		setStatus( 200 );
	}

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

	public List<NGCookie> cookies() {
		return _cookies;
	}

	public void addCookie( NGCookie cookie ) {
		_cookies.add( cookie );
	}

	/**
	 * FIXME: Decide if this should be settable
	 */
	public void setStatus( final int status ) {
		_status = status;
	}

	@Override
	public NGResponse generateResponse() {
		return this;
	}
}