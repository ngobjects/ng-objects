package ng.appserver.http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parses application/x-www-form-urlencoded content (a form's body, or a URL's query string) into form values,
 * following the WHATWG URL standard's parser (https://url.spec.whatwg.org/#urlencoded-parsing):
 *
 * - The input is split on '&' and each field on its first '='. A field without '=' has the empty string as its value, and empty fields are skipped.
 * - '+' is a space, and percent-escapes are decoded to bytes. A '%' that doesn't begin a valid escape is kept as is.
 * - Names and values are then decoded with the given charset; bytes that aren't valid in it become U+FFFD.
 *
 * Values are kept in the order they appear, as are the names.
 */

final class NGFormValues {

	private NGFormValues() {}

	/**
	 * @return The form values in the given input
	 * @throws IllegalArgumentException If the input has more than maxFields fields
	 */
	static Map<String, List<String>> parse( final byte[] input, final Charset charset, final int maxFields ) {
		Objects.requireNonNull( input );
		Objects.requireNonNull( charset );

		final Map<String, List<String>> formValues = new LinkedHashMap<>();

		int fieldCount = 0;
		int start = 0;

		while( start <= input.length ) {
			int end = indexOf( input, (byte)'&', start, input.length );

			if( end == -1 ) {
				end = input.length;
			}

			if( end > start ) {
				fieldCount++;

				if( fieldCount > maxFields ) {
					throw new IllegalArgumentException( "Form content has more than %s fields".formatted( maxFields ) );
				}

				final int equals = indexOf( input, (byte)'=', start, end );

				final String name;
				final String value;

				if( equals == -1 ) {
					name = decode( input, start, end, charset );
					value = "";
				}
				else {
					name = decode( input, start, equals, charset );
					value = decode( input, equals + 1, end, charset );
				}

				formValues.computeIfAbsent( name, _unused -> new ArrayList<>() ).add( value );
			}

			start = end + 1;
		}

		return formValues;
	}

	/**
	 * @return The index of the first occurrence of b in input from start (inclusive) to end (exclusive), -1 if it doesn't occur
	 */
	private static int indexOf( final byte[] input, final byte b, final int start, final int end ) {
		for( int i = start; i < end; i++ ) {
			if( input[i] == b ) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * @return The given range of input with '+' replaced by a space and percent-escapes decoded, read as a string in the given charset
	 */
	private static String decode( final byte[] input, final int start, final int end, final Charset charset ) {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream( end - start );

		for( int i = start; i < end; i++ ) {
			final byte b = input[i];

			if( b == '+' ) {
				bytes.write( ' ' );
			}
			else if( b == '%' && i + 2 < end && hexValue( input[i + 1] ) != -1 && hexValue( input[i + 2] ) != -1 ) {
				bytes.write( hexValue( input[i + 1] ) << 4 | hexValue( input[i + 2] ) );
				i += 2;
			}
			else {
				bytes.write( b );
			}
		}

		return bytes.toString( charset );
	}

	/**
	 * @return The value of the given hex digit, -1 if it isn't one
	 */
	private static int hexValue( final byte b ) {
		if( b >= '0' && b <= '9' ) {
			return b - '0';
		}

		if( b >= 'a' && b <= 'f' ) {
			return b - 'a' + 10;
		}

		if( b >= 'A' && b <= 'F' ) {
			return b - 'A' + 10;
		}

		return -1;
	}
}
