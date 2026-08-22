package ng.appserver;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.templating.NGComponent;

/**
 * The page cache is used by stateful actions to store instances of previously rendered components
 *
 * All access to the cache's maps is synchronized on the cache instance itself. Exclusive access to the cached pages
 * (which is a different thing entirely) is handled by checkout() and lockPage(), which lock the page for the duration of a request.
 *
 * Locking is per page *instance*: however many contexts reference an instance, they share one lock, so requests working
 * with the same page serialize while requests working with different pages of the same session run concurrently.
 * The lock guards what the framework owns — the page instance and it's component tree, which the request-handling phases mutate.
 * Application state shared *between* pages (model objects, the session, pages referencing each other) is, deliberately, the
 * application's to synchronize — we don't serialize the whole session to paper over shared mutable application state.
 * Note that only component action requests check out pages — direct actions and other request types take no page lock.
 *
 * FIXME: We should probably have separate cache entry types for full pages/partial pages. Going to wait with it a bit while we're at the design stage // Hugi 2024-10-03
 * FIXME: On the same note, a page fragment cache entry should probably just reference it's parent's page instance. Page fragments should always be referencing the same instance anyway // Hugi 2024-10-03
 */

public class NGPageCache {

	private static final Logger logger = LoggerFactory.getLogger( NGPageCache.class );

	/**
	 * Maximum number of full page entries kept in the cache. Once reached, the oldest entries get evicted.
	 */
	private final int _pageCacheSize;

	/**
	 * Maximum number of child (partial page update) entries kept for each page. This bounds the cache's growth for pages
	 * that serve a lot of partial updates. Only superseded fragments (fragments whose update container has since been
	 * rendered again) are evicted at the cap — the newest entry for each container is always retained, since it's the
	 * content currently on the user's screen.
	 */
	private final int _fragmentCacheSize;

	/**
	 * How long checkout() will wait to acquire a page's lock before giving up.
	 * Hitting this timeout means a previous request working with the same page still hasn't finished, probably stuck in a long running action.
	 */
	private final Duration _lockTimeout;

	/**
	 * Creates a cache with the default sizes and lock timeout
	 *
	 * FIXME: The defaults should be settable/loaded from Properties // Hugi 2024-03-25
	 */
	public NGPageCache() {
		this( 100, 50, Duration.ofSeconds( 10 ) );
	}

	public NGPageCache( final int pageCacheSize, final int fragmentCacheSize, final Duration lockTimeout ) {
		if( pageCacheSize < 1 ) {
			throw new IllegalArgumentException( "pageCacheSize must be at least 1 (was: %s)".formatted( pageCacheSize ) );
		}

		if( fragmentCacheSize < 1 ) {
			throw new IllegalArgumentException( "fragmentCacheSize must be at least 1 (was: %s)".formatted( fragmentCacheSize ) );
		}

		Objects.requireNonNull( lockTimeout );

		if( lockTimeout.isNegative() || lockTimeout.isZero() ) {
			throw new IllegalArgumentException( "lockTimeout must be positive (was: %s)".formatted( lockTimeout ) );
		}

		_pageCacheSize = pageCacheSize;
		_fragmentCacheSize = fragmentCacheSize;
		_lockTimeout = lockTimeout;
	}

	/**
	 * Represents a single entry in the page cache, along with it's "child entries"
	 * "Child entries" currently means entries generated for update containers within the same page, meaning they can be thrown out with their parent.
	 * Children are keyed by contextID and kept in insertion order, oldest entry first. A child's parent is always the root (full page) entry, keeping parent chains one level deep.
	 *
	 * Only root entries carry a (mutable) children map. Child entries can't have children of their own, so they get a shared empty map.
	 * (Page locks don't live on entries at all — they're keyed by page instance in the cache's lock map, see lockFor())
	 */
	public record NGPageCacheEntry( String contextID, NGComponent page, String originatingContextID, String updateContainerID, NGPageCacheEntry parent, Map<String, NGPageCacheEntry> children ) {

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

		// The record-generated equals/hashCode/toString would recurse infinitely through the parent/children cycle
		// (root → children → child → parent → root), so identity is defined by contextID alone and toString excludes the cyclic components.

		@Override
		public boolean equals( Object other ) {
			return other instanceof NGPageCacheEntry entry && contextID.equals( entry.contextID() );
		}

		@Override
		public int hashCode() {
			return contextID.hashCode();
		}

		@Override
		public String toString() {
			return "NGPageCacheEntry[contextID=%s, page=%s, originatingContextID=%s, updateContainerID=%s, childCount=%s]".formatted( contextID, page.getClass().getName(), originatingContextID, updateContainerID, children.size() );
		}
	}

	/**
	 * Represents exclusive access to a page instance.
	 * Closing the lease releases the lock on the page, allowing other requests to work with it, so always use checkout()/lockPage() in a try-with-resources.
	 */
	public static class NGPageLease implements AutoCloseable {

		private final NGComponent _page;
		private final ReentrantLock _lock;
		private boolean _closed;

		private NGPageLease( final NGComponent page, final ReentrantLock lock ) {
			_page = page;
			_lock = lock;
		}

		public NGComponent page() {
			return _page;
		}

		/**
		 * Releases the lock on the page. Idempotent, since the lock is shared between all leases on the same page instance,
		 * meaning an accidental double unlock could release a lock held by a different lease.
		 */
		@Override
		public void close() {
			if( !_closed ) {
				_closed = true;
				_lock.unlock();
			}
		}
	}

	/**
	 * Root map, stores only full page updates
	 */
	private final Map<String, NGPageCacheEntry> _cacheMap = new LinkedHashMap<>();

	/**
	 * Maps all contextIDs, regardless of whether they're full page updates or partial updates
	 *
	 * FIXME: I don't like having this around, we're going to have to sit down and make a better design of this // Hugi 2024-10-03
	 */
	private final Map<String, NGPageCacheEntry> _allEntries = new HashMap<>();

	/**
	 * Maps page instances to their locks.
	 *
	 * The lock's identity is tied to the page instance itself — not to the cache's entries — so however a request reaches an
	 * instance (through any number of cache entries, or after the instance was evicted and re-saved mid-request), it always
	 * gets the same lock. A WeakHashMap gives us identity semantics (NGComponent doesn't override equals) and a lock that
	 * lives exactly as long as it's page instance is reachable; the values don't reference the keys, so entries don't self-retain.
	 *
	 * Accessed only under the cache's monitor.
	 */
	private final Map<NGComponent, ReentrantLock> _locks = new WeakHashMap<>();

	/**
	 * @return The lock for the given page instance, created on first use. Must be invoked while holding the cache's monitor.
	 */
	private ReentrantLock lockFor( final NGComponent page ) {
		return _locks.computeIfAbsent( page, p -> new ReentrantLock() );
	}

	/**
	 * Saves the given page in the page cache
	 *
	 * @param contextID The ID of the context being stored, used as a key to identify/retrieve the cache entry
	 * @param page The page instance associated with the given context
	 * @param originatingContextID The ID of the context that initiated the creation of context we're about to store. In the case of partial page updates, this key will be used to associate the cache entry with it's "parent"
	 * @param updateContainerID The ID of the update container targeted by the given request, if this was a partial page update
	 */
	public synchronized void savePage( final String contextID, final NGComponent page, final String originatingContextID, final String updateContainerID ) {
		logger.debug( "Saving contextID '{}' with page '{}' originating from context '{}', updateContainerID '{}'", contextID, page.getClass(), originatingContextID, updateContainerID );

		// A little sanity check since if we're storing the same contextID twice, we're probably on our way to do something horrible
		if( _allEntries.containsKey( contextID ) ) {
			throw new IllegalStateException( "Attempted to overwrite page cache key '%s' with component '%s'".formatted( contextID, page.getClass().getName() ) );
		}

		// The entry of the context this save originates from, if present in the cache
		final NGPageCacheEntry originatingEntry = originatingContextID != null ? _allEntries.get( originatingContextID ) : null;

		// The containing page's root entry, if this save is a partial page update.
		//
		// A container-targeted save only becomes a partial entry if the containing page is still in the cache AND the rendered
		// page is actually the same instance — an action targeting an update container may return a different page entirely,
		// which is a full page in it's own right. If the containing page got pushed out of the cache while this request
		// was being handled, the fragment gets promoted to a normally evictable root entry instead: the page instance is alive
		// and on the user's screen, so failing the save would discard work the action has already performed.
		//
		// FIXME: This method of registering the "parent" kind of sucks. Tackle once we migrate to a typed cache (see FIXME in class header) // Hugi 2024-10-03
		NGPageCacheEntry parentEntry = null;

		if( updateContainerID != null && originatingEntry != null && originatingEntry.rootEntry().page() == page ) {
			// The parent is always the containing page's root entry — also for fragments originating from other fragments —
			// keeping parent chains one level deep (so evicted fragment entries don't stay reachable through newer entries' parent references)
			parentEntry = originatingEntry.rootEntry();
		}
		else if( updateContainerID != null && originatingEntry == null ) {
			logger.debug( "Promoting fragment save for contextID '{}' to a root entry, since it's originating context '{}' is no longer present in the cache", contextID, originatingContextID );
		}

		final NGPageCacheEntry cacheEntry;

		if( parentEntry != null ) {
			cacheEntry = new NGPageCacheEntry( contextID, page, originatingContextID, updateContainerID, parentEntry, Map.of() );

			final Map<String, NGPageCacheEntry> children = parentEntry.children();
			children.put( contextID, cacheEntry );

			// While the page's fragment history is over it's cap, evict the oldest superseded fragments (fragments whose update container has since been rendered again).
			// The newest entry for each container is never evicted — it's the content currently on the user's screen. If nothing (more) is superseded
			// (meaning the page genuinely displays that many containers), we allow the history to exceed the cap.
			while( children.size() > _fragmentCacheSize ) {
				final Map<String, String> newestPerContainer = new HashMap<>();

				for( NGPageCacheEntry child : children.values() ) {
					newestPerContainer.put( child.updateContainerID(), child.contextID() );
				}

				NGPageCacheEntry childToEvict = null;

				for( NGPageCacheEntry child : children.values() ) {
					if( !child.contextID().equals( newestPerContainer.get( child.updateContainerID() ) ) ) {
						childToEvict = child;
						break;
					}
				}

				if( childToEvict == null ) {
					break;
				}

				removeEntry( childToEvict );
			}
		}
		else {
			cacheEntry = new NGPageCacheEntry( contextID, page, originatingContextID, null, null, new LinkedHashMap<>() );

			_cacheMap.put( contextID, cacheEntry );

			// While the page cache is over it's cap, remove the oldest entries.
			// The entry just saved is by definition the newest, so it can never be it's own eviction victim.
			while( _cacheMap.size() > _pageCacheSize ) {
				// Since the page cache is a LinkedHashMap (which maintains insertion order), the first entry is the oldest one
				removeEntry( _cacheMap.values().iterator().next() );
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

		// Detach from the containing page's child list, if this is a fragment entry
		if( entry.parent() != null ) {
			entry.parent().children().remove( entry.contextID() );
		}

		// Remove the entry's children from the global context->page map
		entry.children().forEach( ( childContextID, childEntry ) -> {
			_allEntries.remove( childEntry.contextID() );
		} );

		_allEntries.remove( entry.contextID() );
		logger.debug( "Popped contextID '{}' from page cache", entry.contextID() );
	}

	/**
	 * Checks out the page with the given contextID for exclusive use, moving it (or, in the case of a partial page update, it's containing page) to the top of the page cache.
	 *
	 * The returned lease holds the page instance's lock, serializing requests that work with the same page instance, and must be closed once the request has been handled, so use this in a try-with-resources.
	 */
	public NGPageLease checkout( final String contextID ) {
		final NGPageCacheEntry cacheEntry;
		final ReentrantLock lock;

		synchronized( this ) {
			cacheEntry = _allEntries.get( contextID );

			if( cacheEntry == null ) {
				throw new NGPageRestorationException( "No page found in the page cache for contextID '%s'. The page has probably been pushed out of the session's page cache".formatted( contextID ) );
			}

			// Since the page is being worked with, we can safely assume it's become relevant again, so we give it another shot at life by moving it's root entry to the top of the page cache
			final NGPageCacheEntry rootEntry = cacheEntry.rootEntry();

			if( _cacheMap.remove( rootEntry.contextID() ) == null ) {
				throw new IllegalStateException( "Attempted to retain root page cache entry for non-existent contextID '%s'. Probably not your fault, but the framework is doing something it shouldn't be doing".formatted( rootEntry.contextID() ) );
			}

			_cacheMap.put( rootEntry.contextID(), rootEntry );

			lock = lockFor( cacheEntry.page() );
		}

		final NGPageLease lease = acquireLock( cacheEntry.page(), lock );

		logger.debug( "Checked out contextID '{}' with page '{}'", contextID, cacheEntry.page().getClass() );

		return lease;
	}

	/**
	 * Acquires the lock for the given page instance directly, for working with a page outside of checkout() —
	 * the primary case being an action returning an already-cached page instance (held in a component's field),
	 * which must be locked while it's mutated and rendered, since another request may be working with the same instance.
	 *
	 * A request may end up holding two leases (the checked out page's and the response page's). Even if two requests acquire
	 * the same two pages in opposite order, this can't deadlock permanently since acquisition is timed — the worst case is
	 * both requests failing with NGPageContendedException. Timed acquisition is the only available resolution for that case:
	 * the second page isn't known until the action returns it, so acquiring in a fixed lock order isn't an option.
	 * Acquisition is reentrant, so locking the page already checked out by the current thread succeeds immediately.
	 *
	 * Note that a timeout here fails the request *after* the action has executed — and the resulting "try again" response
	 * invites the user to re-invoke that action. That's one more reason the lock timeout should stay generous: it's a last
	 * resort against a stuck request, not a tuning knob.
	 *
	 * The returned lease must be closed once rendering has finished, so use this in a try-with-resources.
	 */
	public NGPageLease lockPage( final NGComponent page ) {
		final ReentrantLock lock;

		synchronized( this ) {
			lock = lockFor( page );
		}

		return acquireLock( page, lock );
	}

	/**
	 * Acquires the given page's lock with the cache's timeout, wrapping it in a lease.
	 */
	private NGPageLease acquireLock( final NGComponent page, final ReentrantLock lock ) {
		// Note that we wait for the lock without holding the cache's monitor.
		// If we'd wait while holding it, the request currently holding the lock would get stuck
		// once it tries to save it's response's page (savePage synchronizes on the cache), deadlocking both requests.
		try {
			if( !lock.tryLock( _lockTimeout.toMillis(), TimeUnit.MILLISECONDS ) ) {
				logger.warn( "Gave up waiting for the lock on page '{}' after {}. A previous request working with the same page is probably stuck in a long running action", page.getClass().getName(), _lockTimeout );
				throw new NGPageContendedException( "The page is still busy handling a previous request. Please wait a moment and try again.", _lockTimeout );
			}
		}
		catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw new NGPageContendedException( "Interrupted while waiting for access to the page.", _lockTimeout, e );
		}

		return new NGPageLease( page, lock );
	}

	/**
	 * Exposed for monitoring the contents of the cache
	 *
	 * The snapshot is deep enough for safe monitoring iteration: each entry is copied with a copy of it's children map,
	 * so monitoring code never iterates a map a request thread might be mutating.
	 *
	 * @return A snapshot of the cache's full page entries as a map of contextID -> cache entry, oldest entry first
	 */
	public synchronized Map<String, NGPageCacheEntry> cacheMap() {
		final Map<String, NGPageCacheEntry> snapshot = new LinkedHashMap<>();

		for( NGPageCacheEntry entry : _cacheMap.values() ) {
			snapshot.put( entry.contextID(), new NGPageCacheEntry( entry.contextID(), entry.page(), entry.originatingContextID(), entry.updateContainerID(), entry.parent(), new LinkedHashMap<>( entry.children() ) ) );
		}

		return snapshot;
	}
}
