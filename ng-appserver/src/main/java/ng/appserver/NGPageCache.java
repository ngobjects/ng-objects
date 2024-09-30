package ng.appserver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The page cache is used by stateful actions to store instances of previously rendered components
 *
 * FIXME: Evacuate all children from page cache when parent is evacuated // Hugi 2024-09-30
 * FIXME: Retain parent entry when a child entry is retained // Hugi 2024-09-30
 * FIXME: The stored map should probably only contain root entries, using a different map to map to "all entries" // Hugi 2024-09-30
 */

public class NGPageCache {

	private static final Logger logger = LoggerFactory.getLogger( NGPageCache.class );

	/**
	 * Represents a single entry in the page cache, along with it's "child entries"
	 * "Child entries" currently means entries generated for update containers within the same page, meaning they can be thrown out with their parent.
	 */
	public record NGPageCacheEntry( String contextID, NGComponent page, String originatingContextID, String updateContainerID, List<NGPageCacheEntry> children ) {

		public NGPageCacheEntry( String contextID, NGComponent page, String originatingContextID, String updateContainerID ) {
			this( contextID, page, originatingContextID, updateContainerID, new ArrayList<>() );
		}
	}

	/**
	 * In the case of component actions, stores the currently active page instance by contextID.
	 */
	private Map<String, NGPageCacheEntry> _cacheMap = new LinkedHashMap<>();

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
	 * @param page The page instance associated with the given context
	 * @param originatingContextID The ID of the context that initiated the creation of context we're about to store. In the case of partial page updates, this key will be used to associate the cache entry with it's "parent"
	 * @param updateContainerID The ID of the update container targeted by the given request. We only need one cached copy for each update container (since an area's content will never get used once it's been replaced)
	 */
	public void savePage( final String contextID, final NGComponent page, final String originatingContextID, final String updateContainerID ) {
		logger.debug( "Saving page '{}' in cache with contextID '{}' originating from context '{}', updateContainerID '{}'", page.getClass(), contextID, originatingContextID, updateContainerID );

		// A little sanity check since if we're storing the same contextID twice, we're probably on our way to do something horrible
		if( _cacheMap.containsKey( contextID ) ) {
			throw new IllegalStateException( "Attempted to overwrite page cache key '%s' with component '%s'".formatted( contextID, page.name() ) );
		}

		final NGPageCacheEntry cacheEntry = new NGPageCacheEntry( contextID, page, originatingContextID, updateContainerID );
		_cacheMap.put( contextID, cacheEntry );

		// If the updateContainerID is not null, we're going to associate this entry with it's parent.
		// FIXME: nulls are not nice. A full page update should probably be represented by an explicit values for both the originating context and the updateContainer // Hugi 2024-09-30
		if( originatingContextID != null && updateContainerID != null ) {
			final NGPageCacheEntry parentEntry = _cacheMap.get( originatingContextID );
			parentEntry.children().add( parentEntry );
		}

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

		final NGPageCacheEntry cacheEntry = _cacheMap.get( contextID );

		// FIXME: This is probably the place to throw a page restoration error instead // Hugi 2024-09-30
		if( cacheEntry == null ) {
			throw new IllegalStateException( "No cached page was found for the contextID '%s'".formatted( contextID ) );
		}

		return cacheEntry.page();
	}

	/**
	 * Moves the page associated with the given contextID to the top of the page cache
	 */
	public void retainPageWithContextIDInCache( final String contextID ) {
		logger.debug( "Retaining contextID {} in cache", contextID );
		final NGPageCacheEntry cacheEntry = _cacheMap.remove( contextID );

		// If we're attempting to retain a non-existent cache entry, odds are we're doing something bad so let's check for that.
		if( cacheEntry == null ) {
			throw new IllegalStateException( "Attempted to retain page cache entry for non-existent contextID '%s'. Probably not your fault, but the framework is doing something it shouldn't be doing".formatted( contextID ) );
		}

		_cacheMap.put( contextID, cacheEntry );
	}

	/**
	 * Exposed for monitoring the contents of the cache
	 *
	 * FIXME: Since this is meant as a read-only view, we might want to return a copy (or an otherwise immutable view) of the actual map to prevent any outside meddling // Hugi 2024-09-30
	 *
	 * @return The contents of the cache as a map of contextID -> cache entry
	 */
	public Map<String, NGPageCacheEntry> cacheMap() {
		return _cacheMap;
	}
}