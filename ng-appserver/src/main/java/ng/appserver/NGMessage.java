package ng.appserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Parent class of NGResponse/NGRequest
 */

public abstract class NGMessage implements NGMessageInterface {

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
	private Map<String, List<String>> _headers = NGMessageInterface.createEmptyHeadersMap();

	/**
	 * The content of this message
	 *
	 * FIXME:
	 * Currently this stores all types of content. We're going to want to use more efficient types for different response types (string/data/streaming)
	 * For example, it's clear that using a StringBuilder for string responses is significantly more efficient than using the ByteArrayOutputStream
	 * // Hugi 2023-02-08
	 */
	private ByteArrayOutputStream _contentByteStream = new ByteArrayOutputStream( DEFAULT_CONTENT_DATA_LENGTH );

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
	@Override
	public Map<String, List<String>> headers() {
		return _headers;
	}

	/**
	 * Sets the headers from the given map.
	 */
	public void setHeaders( final Map<String, List<String>> newHeaders ) {
		_headers = NGMessageInterface.createEmptyHeadersMap();

		for( Entry<String, List<String>> header : newHeaders.entrySet() ) {
			_headers.put( header.getKey(), header.getValue() );
		}
	}

	/**
	 * @return The headers matching the given key
	 */
	public List<String> headersForKey( final String key ) {
		final List<String> values = headers().get( key );

		if( values == null ) {
			return Collections.emptyList();
		}

		return values;
	}

	/**
	 * @return The header matching the given key
	 */
	public String headerForKey( final String key ) {
		final List<String> values = headersForKey( key );

		if( values.isEmpty() ) {
			return null;
		}

		// Fail if multiple header values are present
		if( values.size() > 1 ) {
			// FIXME: We should be failing here // Hugi 2026-03-03
			//			throw new IllegalStateException( "The request contains %s headers '%s' with values (%s). If you expected multiple header values, use headersForKey() instead of headerForKey()".formatted( values.size(), key, values ) );
		}

		return values.get( 0 );
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
		return contentByteStream().toByteArray();
	}

	/**
	 * @return The length of the message's data content
	 */
	public long contentBytesLength() {
		return contentByteStream().size();
	}

	/**
	 * @return The response's content stream
	 */
	public ByteArrayOutputStream contentByteStream() {
		return _contentByteStream;
	}

	public void _setContentByteStream( ByteArrayOutputStream value ) {
		_contentByteStream = value;
	}

	public void setContentBytes( final byte[] contentBytes ) {
		_setContentByteStream( new ByteArrayOutputStream( DEFAULT_CONTENT_DATA_LENGTH ) );
		appendContentBytes( contentBytes );
	}

	private void appendContentBytes( final byte[] contentBytes ) {
		try {
			contentByteStream().write( contentBytes );
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}