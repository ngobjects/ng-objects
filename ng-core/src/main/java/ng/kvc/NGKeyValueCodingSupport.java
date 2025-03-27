package ng.kvc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility methods for supporting KVC usage/features
 */

public class NGKeyValueCodingSupport {

	/**
	 * @return a List of keys that can be invoked on the given object
	 */
	private static List<String> availableGetterKeysForObject( final Object object ) {
		return availableGetterKeysForClass( object.getClass() );
	}

	/**
	 * @return A list of available accessible keys on the given object
	 */
	private static List<String> availableGetterKeysForClass( final Class<?> objectClass ) {
		final List<String> result = new ArrayList<>();

		for( final Method method : objectClass.getMethods() ) {
			if( isGetterMethod( method ) ) {
				result.add( keyForMethodName( method.getName() ) );
			}
		}

		for( Field field : objectClass.getFields() ) {
			result.add( field.getName() );
		}

		return result;
	}

	/**
	 * @return true if the method is a valid KVC getter method. That means the method accepts no parameters and does not have a return type of Void
	 */
	private static boolean isGetterMethod( final Method method ) {

		// Methods that take parameters can't be used as a KVC getter
		if( method.getParameterCount() != 0 ) {
			return false;
		}

		// A method that returns nothing (void) can't be used as a KVC getter
		if( method.getReturnType().isAssignableFrom( Void.class ) ) {
			return false;
		}

		return true;
	}

	/**
	 * @return A java method name, normalized for use in a KeyPath. This means removing a prefixing underscore or "get" (And in case of "get", lowercasing the following character)
	 */
	static String keyForMethodName( final String methodName ) {

		String key = methodName;

		// A prefixing underscore always gets removed, regardless of if followed by a different prefix such as "get" or "is"
		if( methodName.charAt( 0 ) == '_' ) {
			key = methodName.substring( 1 );
		}

		if( key.startsWith( "get" ) ) {
			return keyByRemovingPrefix( key, "get" );
		}

		if( key.startsWith( "is" ) ) {
			return keyByRemovingPrefix( key, "is" );
		}

		return key;
	}

	/**
	 * @return The given key by removing the given prefix, and lowercasing the first letter in the remainder
	 */
	private static String keyByRemovingPrefix( String key, final String prefix ) {

		// Methods named "get" or "is" are just fine
		if( key.equals( prefix ) ) {
			return key;
		}

		// Remove the prefix
		key = key.substring( prefix.length() );

		// Lowercase the first letter of the remainder
		key = key.substring( 0, 1 ).toLowerCase() + key.substring( 1, key.length() );

		return key;
	}

	/**
	 * @return A list of suggestions for the given key when trying to apply it to the given object. Really just a list of the object's available keys, ordered by the edit distance from the proposed key
	 */
	public static List<String> suggestions( final Object object, final String proposedKey ) {

		record Suggestion( int distance, String key ) {}

		return availableGetterKeysForObject( object )
				.stream()
				.map( key -> new Suggestion( distanceLevenshtein( key, proposedKey ), key ) )
				.sorted( Comparator.comparing( Suggestion::distance ) )
				.map( Suggestion::key )
				.toList();
	}

	/**
	 * @return The Levenshtein (edit) distance between two strings
	 */
	private static int distanceLevenshtein( String a, String b ) {
		a = a.toLowerCase();
		b = b.toLowerCase();

		// i == 0
		final int[] costs = new int[b.length() + 1];
		for( int j = 0; j < costs.length; j++ ) {
			costs[j] = j;
		}

		for( int i = 1; i <= a.length(); i++ ) {
			// j == 0; nw = lev(i - 1, j)
			costs[0] = i;
			int nw = i - 1;
			for( int j = 1; j <= b.length(); j++ ) {
				final int cj = Math.min( 1 + Math.min( costs[j], costs[j - 1] ), a.charAt( i - 1 ) == b.charAt( j - 1 ) ? nw : nw + 1 );
				nw = costs[j];
				costs[j] = cj;
			}
		}

		return costs[b.length()];
	}
}