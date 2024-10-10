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
	 * @return A list of available accessible keys on the given object
	 *
	 * FIXME: Currently only using exact keys, missing all the KVC method name munging, strip prefixes (underbar, "get") etc. // Hugi 2024-10-10
	 * FIXME: Altogether experimental, we'll have to have a better look at this design-wise // Hugi 2024-10-10
	 * FIXME: We're probably going to want a separate method that can check instances as well (for example, applicable when we add support for rseolving keys on Maps) // Hugi 2024-10-10
	 */
	public static List<String> availableKeyPaths( final Class<?> objectClass ) {
		final List<String> result = new ArrayList<>();

		for( Method method : objectClass.getClass().getMethods() ) {
			if( method.getParameterCount() == 0 ) {
				if( !method.getReturnType().isAssignableFrom( Void.class ) ) {
					result.add( method.getName() );
				}
			}
		}

		for( Field field : objectClass.getClass().getFields() ) {
			result.add( field.getName() );
		}

		return result;
	}

	/**
	 * @return A list of suggestions for the given key when trying to apply it to the given object. Really just a list of the object's available keys, ordered by the edit distance from the proposed key
	 */
	public static List<String> suggestions( final Object object, final String proposedKey ) {

		record Suggestion( int distance, String key ) {}

		return availableKeyPaths( object.getClass() )
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