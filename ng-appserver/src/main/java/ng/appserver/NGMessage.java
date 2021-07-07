package ng.appserver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parent class of NGResponse/NGRequest
 *
 * Currently this just stores a byte array for the content.
 * We probably want to change that to eventually be a stream as well.
 * Or even a string, to prevent constant conversion between types when serving string responses.
 */

public abstract class NGMessage {

	/**
	 * Stores the HTTP version of this request/respons
	 */
	private String _httpVersion = "HTTP/1.0"; // FIXME: We shouldn't be hardcoding this value here

	/**
	 * FIXME: Probably don't want to populate this with an empty map at the start?
	 * FIXME: Thread safety!
	 */
	private Map<String, List<String>> _headers = new HashMap<>();

	byte[] _contentBytes = new byte[] {};

	public String httpVersion() {
		return _httpVersion;
	}

	public void setHttpVersion( final String value ) {
		_httpVersion = value;
	}

	public Map<String, List<String>> headers() {
		return _headers;
	}

	public void setHeaders( Map<String, List<String>> headers ) {
		_headers = headers;
	}

	public void setHeader( final String key, final String value ) {
		List<String> list = headers().get( key );

		// FIXME: This implicitly mutable thing is... not good
		if( list == null ) {
			list = new ArrayList<>();
			headers().put( key, list );
		}

		list.add( value );
	}

	public String contentString() {
		return new String( contentBytes(), StandardCharsets.UTF_8 );
	}

	public void setContentString( final String contentString ) {
		setContentBytes( contentString.getBytes( StandardCharsets.UTF_8 ) );
	}

	public void appendContentString( final String stringToAppend ) {
		setContentString( contentString().concat( stringToAppend ) );
	}

	public void setContentBytes( final byte[] contentBytes ) {
		_contentBytes = contentBytes;
	}

	public byte[] contentBytes() {
		return _contentBytes;
	}
}