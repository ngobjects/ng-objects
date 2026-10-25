package ng.appserver.http;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGActionResults;

/**
 * FIXME: Need to decide what to do about responses of different types.
 * Is a string response the same type as a binary response or a streaming response, or should these have different implementations
 * Are responses even mutable? A mutable response should possibly have a different design or a builder
 * // Hugi 2022-06-05
 */

public interface NGResponse extends NGActionResults {

	public int status();

	public void setStatus( final int status );

	public List<NGCookie> cookies();

	public void addCookie( final NGCookie cookie );

	/**
	 * @return The response's headers, by name. Names are case-insensitive.
	 */
	public Map<String, List<String>> headers();

	/**
	 * Sets the headers from the given map.
	 */
	public void setHeaders( final Map<String, List<String>> newHeaders );

	/**
	 * @return The values of the header with the given name. An empty list if there are none.
	 */
	public default List<String> headersForKey( final String key ) {
		final List<String> values = headers().get( key );

		if( values == null ) {
			return Collections.emptyList();
		}

		return values;
	}

	/**
	 * @return The first value of the header with the given name. null if there's none.
	 */
	public default String headerForKey( final String key ) {
		final List<String> values = headersForKey( key );

		if( values.isEmpty() ) {
			return null;
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

	public void appendContentString( final String stringToAppend );

	public void setContentBytes( final byte[] contentBytes );

	public void setContentString( final String contentString );

	/**
	 * @return The response's content
	 *
	 * FIXME: Reading content back leaves this interface for the implementations that store it, once rendering a component to a string has an API of its own (#104) // Hugi/Claude 2026-10-25
	 */
	public byte[] contentBytes();

	/**
	 * @return The response's content, as a UTF-8 string
	 */
	public default String contentString() {
		return new String( contentBytes(), StandardCharsets.UTF_8 );
	}
}