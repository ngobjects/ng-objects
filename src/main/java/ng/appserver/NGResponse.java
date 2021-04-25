package ng.appserver;

import java.nio.charset.StandardCharsets;

/**
 * FIXME:
 * Need to decide what to do about responses of different types.
 * Is a string response the same type as a binary response or a streaming response, even?
 * Are responses even mutable?
 */

public class NGResponse extends NGMessage {

	/**
	 * FIXME: Decide if we want a default 
	 */
	private int _status = 200;

	private String _contentString;
	
	public NGResponse( final String string ) {
		_contentString = string;
	}
	
	public String contentString() {
		return _contentString;
	}
	
	public void setContentString( final String contentString ) {
		_contentString = contentString;
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

	/**
	 * FIXME: This should handle more than just bytes 
	 */
	public byte[] bytes() {
		return contentString().getBytes( StandardCharsets.UTF_8 );
	}
}