package ng.appserver;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface NGMessageInterface {

	/**
	 * Creates an empty map to store headers.
	 * Separate method since we might want to change the map type later.
	 */
	public static Map<String, List<String>> createEmptyHeadersMap() {
		return new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
	}

	public Map<String, List<String>> headers();
}