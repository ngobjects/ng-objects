package ng.appserver.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parses the values of a request's Cookie headers (RFC 6265: "name=value; name2=value2") into cookie values, leniently:
 *
 * - Each header is split on ';', and each pair on its first '='. Whitespace around names and values is ignored.
 * - A value wrapped in double quotes loses the quotes. Values are otherwise kept as they are: cookie values are opaque, so nothing is decoded.
 * - A pair without '=', or with an empty name, isn't a cookie and is skipped. The rest of the header is still read.
 * - Names are case-sensitive. A name sent more than once keeps every value, in the order sent.
 *
 * HTTP/2 clients may send several Cookie headers, which together hold the request's cookies.
 */

final class NGCookieValues {

	private NGCookieValues() {}

	/**
	 * @param headerValues The values of the request's Cookie headers
	 * @return The cookie values in the given headers, by name
	 */
	static Map<String, List<String>> parse( final List<String> headerValues ) {
		Objects.requireNonNull( headerValues );

		final Map<String, List<String>> cookieValues = new LinkedHashMap<>();

		for( final String headerValue : headerValues ) {
			for( final String pair : headerValue.split( ";" ) ) {
				final int equals = pair.indexOf( '=' );

				if( equals == -1 ) {
					continue;
				}

				final String name = pair.substring( 0, equals ).strip();

				if( name.isEmpty() ) {
					continue;
				}

				String value = pair.substring( equals + 1 ).strip();

				if( value.length() >= 2 && value.startsWith( "\"" ) && value.endsWith( "\"" ) ) {
					value = value.substring( 1, value.length() - 1 );
				}

				cookieValues.computeIfAbsent( name, _unused -> new ArrayList<>() ).add( value );
			}
		}

		return cookieValues;
	}
}
