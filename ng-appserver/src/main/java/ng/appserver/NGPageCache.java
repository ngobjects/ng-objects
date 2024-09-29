package ng.appserver;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The page cache is used by stateful actions to store instances of previously rendered components
 */

public class NGPageCache {

	private static final Logger logger = LoggerFactory.getLogger( NGPageCache.class );

	/**
	 * In the case of component actions, stores the currently active page instance by contextID.
	 */
	private Map<String, NGComponent> _cacheMap = new LinkedHashMap<>();

	/**
	 * @return Size of the page cache
	 *
	 * FIXME: Temporary location for this parameter, should be settable/loaded from Properties // Hugi 2024-023-25
	 */
	private int pageCacheSize() {
		return 100;
	}

	/**
	 * Saves the given page in the page cache
	 *
	 * @param contextID The ID of the context being stored, used as a key to identify/retrieve the cache entry
	 * @param page The page associated with the given context
	 * @param originatingContextID The ID of the context that initiated the creation of the given context. In the case of partial page updates, this key is used to associate the cache entry with the actual key
	 * @param updateContainerID The ID of the update container targeted with the given request. We only need one cached copy for each update container (since an area's content will never get used once it's been replaced)
	 */
	public void savePage( final String contextID, final NGComponent page, final String originatingContextID, final String updateContainerID ) {
		logger.debug( "Saving page '{}' in cache with contextID '{}' originating from context '{}', updateContainerID '{}'", page.getClass(), contextID, originatingContextID, updateContainerID );

		// A little sanity check since if we're storing the same contextID twice, we're probably on our way to do something horrible
		if( _cacheMap.containsKey( contextID ) ) {
			throw new IllegalStateException( "Attempted to overwrite page cache key '%s' with component '%s'".formatted( contextID, page.name() ) );
		}

		_cacheMap.put( contextID, page );

		// If the page cache size has been reached, remove the oldest entry
		if( _cacheMap.size() > pageCacheSize() ) {
			// Since the page cache is a LinkedHashMap (which maintains insertion order), the first entry should be the oldest one
			final String oldestEntryKey = _cacheMap.keySet().iterator().next();

			// Bye bye
			_cacheMap.remove( oldestEntryKey );
			logger.debug( "Popped contextID {} from page cache", page.getClass(), oldestEntryKey );
		}
	}

	/**
	 * @return The cached page instance with the given contextID
	 */
	public NGComponent restorePageFromCache( final String contextID ) {
		logger.debug( "Restoring page from cache with contextID: " + contextID );
		return _cacheMap.get( contextID );
	}

	/**
	 * Moves the page associated with the given contextID to the top of the page cache
	 */
	public void retainPageWithContextIDInCache( final String contextID ) {
		logger.debug( "Retaining contextID {} in cache", contextID );
		final NGComponent component = _cacheMap.remove( contextID );
		_cacheMap.put( contextID, component );
	}
}
