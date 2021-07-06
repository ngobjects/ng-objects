package ng.appserver;

import java.nio.charset.StandardCharsets;

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

	/**
	 * FIXME: The response's content should probably be encapsulated by a stream.
	 * FIXME: Do we actually want to initialize this to an empty byte array?
	 */
	private byte[] _contentBytes = new byte[] {};

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

	public String contentString() {
		return new String( contentBytes(), StandardCharsets.UTF_8 );
	}

	public void setContentString( final String contentString ) {
		setContentBytes( contentString.getBytes( StandardCharsets.UTF_8 ) );
	}

	/**
	 * FIXME: Extremely inefficient
	 */
	public void appendContentString( final String stringToAppend ) {
		setContentString( contentString().concat( stringToAppend ) );
	}

	public int status() {
		return _status;
	}

	/**
	 * FIXME: Decide if this should be settable
	 */
	private void setStatus( final int status ) {
		_status = status;
	}

	private void setContentBytes( final byte[] contentBytes ) {
		_contentBytes = contentBytes;
	}

	/**
	 * FIXME: This should handle more than just bytes
	 */
	public byte[] contentBytes() {
		return _contentBytes;
	}

	@Override
	public NGResponse generateResponse() {
		return this;
	}
}