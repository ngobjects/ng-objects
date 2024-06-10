package ng.appserver.resources;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles caching of dynamically constructed resources
 *
 *   CHECKME: I have a sneaking suspicion this will eventually merge with NGResourceManager. Let's just see how the stars/designs end upp aligning // Hugi 2024-06-10
 */

public class NGResourceManagerDynamic {

	/**
	 * Storage of dynamic data.
	 *
	 * FIXME: This is currently just a regular HashMap, so we're storing resources indefinitely if they're never "popped" (i.e. read)
	 * We're going to have to think about how best to approach a solution to this problem, since different resources might need different cache "scopes".
	 * At first thought a request/context scoped cache sounds like a sensible default.
	 * Other scopes could be session/application wide. Or custom? Hmm.
	 * // Hugi 2023-02-17
	 */
	private Map<String, NGDynamicResource> _cacheMap = new ConcurrentHashMap<>();

	public void push( final String cacheKey, final NGDynamicResource data ) {
		Objects.requireNonNull( cacheKey );
		_cacheMap.put( cacheKey, data );
	}

	public NGDynamicResource pop( final String cacheKey ) {
		return _cacheMap.remove( cacheKey );
	}
}