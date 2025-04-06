package ng.appserver;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.templating.NGComponent;

/**
 * The page cache is used by stateful actions to store instances of previously rendered components
 *
 * FIXME: We should probably have separate cache entry types for full pages/partial pages. Going to wait with it a bit while we're at the design stage // Hugi 2024-10-03
 * FIXME: On the same note, a page fragment cache entry should probably just reference it's parent's page instance. Page fragments should always be referencing the same instance anyway // Hugi 2024-10-03
 */

public class NGPageCache {

	private static final Logger logger = LoggerFactory.getLogger( NGPageCache.class );

	/**
	 * Represents a single entry in the page cache, along with it's "child entries"
	 * "Child entries" currently means entries generated for update containers within the same page, meaning they can be thrown out with their parent.
	 */
	public record NGPageCacheEntry( String contextID, NGComponent page, String originatingContextID, String updateContainerID, NGPageCacheEntry parent, Map<String, NGPageCacheEntry> children, Lock lock ) {

		public NGPageCacheEntry( String contextID, NGComponent page, String originatingContextID, NGPageCacheEntry parent, String updateContainerID ) {
			this( contextID, page, originatingContextID, updateContainerID, parent, new LinkedHashMap<>(), new ReentrantLock() );
		}

		/**
		 * @return true if this was a partial page update
		 */
		public boolean isPartial() {
			// FIXME: nulls are not nice. A full page update should probably be represented by explicit values for both the originating context and the updateContainer // Hugi 2024-09-30
			return originatingContextID != null && updateContainerID != null;
		}

		/**
		 * @return The root page cache entry (i.e. if this is a partial page update, returns the cache entry for the actual page this fragment is on)
		 */
		public NGPageCacheEntry rootEntry() {
			NGPageCacheEntry root = this;

			while( root.parent() != null ) {
				root = root.parent();
			}

			return root;
		}
	}

	/**
	 * Root map, stores only full page updates
	 */
	private Map<String, NGPageCacheEntry> _cacheMap = new LinkedHashMap<>();

	/**
	 * Maps all contextIDs, regardless of whether they're full page updates or partial updates
	 *
	 * FIXME: I don't like having this around, we're going to have to sit down and make a better design of this // Hugi 2024-10-03
	 */
	private Map<String, NGPageCacheEntry> _allEntries = new HashMap<>();

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
		logger.debug( "Saving contextID '{}' with page '{}' originating from context '{}', updateContainerID '{}'", contextID, page.getClass(), originatingContextID, updateContainerID );

		// A little sanity check since if we're storing the same contextID twice, we're probably on our way to do something horrible
		if( _cacheMap.containsKey( contextID ) ) {
			throw new IllegalStateException( "Attempted to overwrite page cache key '%s' with component '%s'".formatted( contextID, page.name() ) );
		}

		// Represents the containing page, if this is a partial page update
		NGPageCacheEntry parentEntry = null;

		// FIXME: This method of registering the "parent" kind of sucks. Tackle once we migrate to a typed cache (see FIXME in class header) // Hugi 2024-10-03
		if( originatingContextID != null && updateContainerID != null ) {
			parentEntry = _allEntries.get( originatingContextID );
		}

		final NGPageCacheEntry cacheEntry = new NGPageCacheEntry( contextID, page, originatingContextID, parentEntry, updateContainerID );

		// In case of partial updates, the cache entry will get stored with it's parent entry, keyed by the ID of the updateContainer
		if( cacheEntry.isPartial() ) {
			cacheEntry.rootEntry().children().put( cacheEntry.updateContainerID(), cacheEntry );
		}
		else {
			_cacheMap.put( contextID, cacheEntry );

			// If the page cache size has been reached, remove the oldest entry
			if( _cacheMap.size() > pageCacheSize() ) {
				// Since the page cache is a LinkedHashMap (which maintains insertion order), the first entry should be the oldest one
				final NGPageCacheEntry oldestEntry = _cacheMap.values().iterator().next();
				removeEntry( oldestEntry );
			}
		}

		_allEntries.put( contextID, cacheEntry );
	}

	/**
	 * Removes the given entry from the page cache, along with all it's child entries
	 */
	private void removeEntry( NGPageCacheEntry entry ) {
		logger.debug( "Removing contextID '{}' with page '{}' originating from context '{}', updateContainerID '{}'", entry.contextID(), entry.page().getClass(), entry.originatingContextID(), entry.updateContainerID() );

		_cacheMap.remove( entry.contextID() );

		// Remove the parent's children from the global context->page map
		entry.children().forEach( ( childContextID, childEntry ) -> {
			_allEntries.remove( childEntry.contextID() );
		} );

		_allEntries.remove( entry.contextID() );
		logger.debug( "Popped contextID '{}' from page cache", entry.contextID() );
	}

	/**
	 * @return The cached page instance with the given contextID
	 */
	public NGComponent restorePageFromCache( final String contextID ) {
		logger.debug( "Restoring page from cache with contextID: " + contextID );

		final NGPageCacheEntry cacheEntry = _allEntries.get( contextID );

		if( cacheEntry == null ) {
			throw new NGPageRestorationException( "No page found in the page cache for contextID '%s'. The page has probably been pushed out of the session's page cache".formatted( contextID ) );
		}

		cacheEntry.lock().lock();
		return cacheEntry.page();
	}

	/**
	 * Release the lock on the context.
	 *
	 * FIXME: Locking in the page cache is still very experimental functionality. Needs testing // Hugi 2025-04-06
	 */
	public void releaseLock( final String contextID ) {
		_allEntries.get( contextID ).lock().unlock();
	}

	/**
	 * In the case of a full page update, moves the entry to the top of the page cache.
	 * In the case of a partial update, moves the parent entry to the top of the page cache.
	 */
	public void retainPageWithContextIDInCache( final String contextID ) {
		logger.debug( "Retaining contextID {} in cache", contextID );

		final NGPageCacheEntry cacheEntry = _allEntries.get( contextID );

		if( cacheEntry.isPartial() ) {
			logger.debug( "contextID '{}' is partial, so we'll retain it's root entry '{}' instead", cacheEntry.contextID(), cacheEntry.originatingContextID() );
			retainRootEntry( cacheEntry.rootEntry() );
		}
		else {
			retainRootEntry( cacheEntry );
		}
	}

	/**
	 * Retains the entry in the cache. Currently, only root entries can be retained, since child entries are retained for the lifetime of the parent
	 */
	private void retainRootEntry( NGPageCacheEntry cacheEntry ) {
		final NGPageCacheEntry rootEntry = _cacheMap.remove( cacheEntry.contextID() );

		// If we're attempting to retain a non-existent root entry, odds are we're doing something bad so let's check for that.
		if( rootEntry == null ) {
			throw new IllegalStateException( "Attempted to retain root page cache entry for non-existent contextID '%s'. Probably not your fault, but the framework is doing something it shouldn't be doing".formatted( cacheEntry.contextID() ) );
		}

		_cacheMap.put( cacheEntry.contextID(), rootEntry );
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