package ng.appserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Parent class of NGResponse/NGRequest
 */

public abstract class NGMessage {

	/**
	 * Arbitrarily picked default length we initialize the size of the content data byte[] with.
	 */
	private static final int DEFAULT_CONTENT_DATA_LENGTH = 8192;

	/**
	 * The HTTP version of this message
	 */
	private String _httpVersion = "HTTP/1.0";

	/**
	 * The headers  of this message
	 */
	private Map<String, List<String>> _headers = _createEmptyHeadersMap();

	/**
	 * The content of this message
	 *
	 * FIXME:
	 * Currently this stores all types of content. We're going to want to use more efficient types for different response types (string/data/streaming)
	 * For example, it's clear that using a StringBuilder for string responses is significantly more efficient than using the ByteArrayOutputStream
	 * // Hugi 2023-02-08
	 */
	private ByteArrayOutputStream _contentBytes = new ByteArrayOutputStream( DEFAULT_CONTENT_DATA_LENGTH );

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
	 * Creates an empty map to store headers.
	 * Separate method since we might want to change the map type later.
	 */
	private static Map<String, List<String>> _createEmptyHeadersMap() {
		return new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
	}

	/**
	 * Sets the headers from the given map.
	 *
	 * FIXME: We're currently copying the map entries to a TreeMap to get case insensitivity. This is not the most efficient implementation // Hugi 2022-01-03
	 */
	public void setHeaders( final Map<String, List<String>> newHeaders ) {
		_headers = _createEmptyHeadersMap();

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
		appendContentBytes( stringToAppend.getBytes( StandardCharsets.UTF_8 ) );
	}

	public byte[] contentBytes() {
		return _contentBytes.toByteArray();
	}

	/**
	 * @return The length of the message's data content
	 *
	 * FIXME: Not sure if we want this here. Currently only for performance while setting the content-length header. // Hugi 2023-02-17
	 */
	public long contentBytesLength() {
		return _contentBytes.size();
	}

	/**
	 * @return The response's content stream
	 *
	 * FIXME: Not sure I want this as public API, but it prevents the adaptor from having to do a copy of the stream's byte array // Hugi 2023-02-17
	 */
	public ByteArrayOutputStream contentByteStream() {
		return _contentBytes;
	}

	public void setContentBytes( final byte[] contentBytes ) {
		_contentBytes = new ByteArrayOutputStream( DEFAULT_CONTENT_DATA_LENGTH );
		appendContentBytes( contentBytes );
	}

	public void appendContentBytes( final byte[] contentBytes ) {
		try {
			_contentBytes.write( contentBytes );
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}