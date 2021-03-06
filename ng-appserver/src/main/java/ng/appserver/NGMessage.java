package ng.appserver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Parent class of NGResponse/NGRequest
 *
 * Currently this just stores a byte array for the content.
 * We probably want to change that to eventually be a stream as well.
 * Or even a string, to prevent constant conversion between types when serving string responses.
 */

public abstract class NGMessage {

	/**
	 * The HTTP version of this message
	 */
	private String _httpVersion = "HTTP/1.0";

	/**
	 * The headers  of this message
	 */
	private Map<String, List<String>> _headers = _createHeadersMap();

	/**
	 * The content of this message
	 */
	byte[] _contentBytes = new byte[] {};

	/**
	 * @return The HTTP version of this message
	 */
	public String httpVersion() {
		return _httpVersion;
	}

	/**
	 * Set the HTTP version of this message
	 */
	public void setHttpVersion( final String value ) {
		_httpVersion = value;
	}

	/**
	 * @return The HTTP headers of this message
	 */
	public Map<String, List<String>> headers() {
		return _headers;
	}

	/**
	 * Sets the headers from the given map.
	 *
	 * FIXME: We're currently copying the map entries to a TreeMap to get case insensitivity. This is not the most efficient implementation // Hugi 2022-01-03
	 */
	public void setHeaders( final Map<String, List<String>> newHeaders ) {
		_headers = _createHeadersMap();

		for( Entry<String, List<String>> header : newHeaders.entrySet() ) {
			_headers.put( header.getKey(), header.getValue() );
		}
	}

	/**
	 * Set the header with the given name to the given value.
	 * Replaces any existing values of the given header.
	 */
	public void setHeader( final String headerName, final String value ) {
		Objects.requireNonNull( headerName );
		Objects.requireNonNull( value );
		headers().put( headerName, List.of( value ) );
	}

	/**
	 * Adds the given value to the named header.
	 * Existing header values are maintained and the new value is added to the end of the value list.
	 */
	public void appendHeader( final String headerName, final String value ) {
		Objects.requireNonNull( headerName );
		Objects.requireNonNull( value );

		List<String> list = headers().get( headerName );

		if( list == null ) {
			list = new ArrayList<>();
			headers().put( headerName, list );
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

	/**
	 * Creates the initial headers map.
	 * Kept a separate method since we might want to change this to a different map type later.
	 */
	private static Map<String, List<String>> _createHeadersMap() {
		return new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
	}
}