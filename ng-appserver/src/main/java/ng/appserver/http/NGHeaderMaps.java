package ng.appserver.http;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Creates the maps requests and responses keep their headers in
 */

final class NGHeaderMaps {

	private NGHeaderMaps() {}

	/**
	 * @return An empty map for headers, whose keys (header names) are case-insensitive
	 */
	static Map<String, List<String>> create() {
		return new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
	}
}
