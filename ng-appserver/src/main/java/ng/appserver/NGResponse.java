package ng.appserver;

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