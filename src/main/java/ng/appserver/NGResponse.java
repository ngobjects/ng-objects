package ng.appserver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FIXME:
 * Need to decide what to do about responses of different types.
 * Is a string response the same type as a binary response or a streaming response, even?
 * Are responses even mutable?
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
	private byte[] _bytes = new byte[] {};

	public NGResponse() {
		setStatus( 200 );
	}

	public NGResponse( final byte[] bytes, final int status ) {
		setBytes( bytes );
		setStatus( status );
	}

	public NGResponse( final String contentString, final int status ) {
		setContentString( contentString );
		setStatus( status );
	}

	public String contentString() {
		return new String( bytes(), StandardCharsets.UTF_8 );
	}

	public void setContentString( final String contentString ) {
		setBytes( contentString.getBytes( StandardCharsets.UTF_8 ) );
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

	private void setBytes( final byte[] bytes ) {
		_bytes = bytes;
	}

	/**
	 * FIXME: This should handle more than just bytes 
	 */
	public byte[] bytes() {
		return _bytes;
	}

	@Override
	public NGResponse generateResponse() {
		return this;
	}
}