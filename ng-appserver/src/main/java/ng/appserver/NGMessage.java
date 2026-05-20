package ng.appserver;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public interface NGMessage {

	/**
	 * Arbitrarily picked default length we initialize the size of the content data byte[] with.
	 */
	public static final int DEFAULT_CONTENT_DATA_LENGTH = 8192;

	/**
	 * Sets the headers from the given map.
	 */
	public void setHeaders( final Map<String, List<String>> newHeaders );

	/**
	 * @return The response's content stream
	 */
	public ByteArrayOutputStream contentByteStream();

	public void _setContentByteStream( ByteArrayOutputStream value );

	public Map<String, List<String>> headers();

	/**
	 * Creates an empty map to store headers.
	 * Separate method since we might want to change the map type later.
	 */
	public static Map<String, List<String>> createEmptyHeadersMap() {
		return new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
	}

	/**
	 * @return The headers matching the given key
	 */
	public default List<String> headersForKey( final String key ) {
		final List<String> values = headers().get( key );

		if( values == null ) {
			return Collections.emptyList();
		}

		return values;
	}

	/**
	 * @return The header matching the given key
	 */
	public default String headerForKey( final String key ) {
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
	public default void setHeader( final String headerName, final String value ) {
		Objects.requireNonNull( headerName );
		Objects.requireNonNull( value );
		headers().put( headerName, List.of( value ) );
	}

	/**
	 * Adds the given value to the named header.
	 * Existing header values are maintained and the new value is added to the end of the value list.
	 */
	public default void appendHeader( final String headerName, final String value ) {
		Objects.requireNonNull( headerName );
		Objects.requireNonNull( value );

		List<String> list = headers().get( headerName );

		if( list == null ) {
			list = new ArrayList<>();
			headers().put( headerName, list );
		}

		list.add( value );
	}

	public default String contentString() {
		return new String( contentBytes(), StandardCharsets.UTF_8 );
	}

	public default byte[] contentBytes() {
		return contentByteStream().toByteArray();
	}
}