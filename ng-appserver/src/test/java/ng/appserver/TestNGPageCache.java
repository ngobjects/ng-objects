package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import ng.appserver.NGPageCache.NGPageCacheEntry;
import ng.appserver.NGPageCache.NGPageLease;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGStandardRequest;
import ng.appserver.templating.NGComponent;

public class TestNGPageCache {

	/**
	 * Page cache size used by the caches under test (kept small to keep the tests brisk)
	 */
	private static final int PAGE_CACHE_SIZE = 10;

	/**
	 * Fragment history cap used by the caches under test
	 */
	private static final int FRAGMENT_CACHE_SIZE = 5;

	private static NGPageCache newCache() {
		return new NGPageCache( PAGE_CACHE_SIZE, FRAGMENT_CACHE_SIZE, Duration.ofSeconds( 10 ) );
	}

	private static NGComponent newPage() {
		final NGRequest request = new NGStandardRequest( "GET", "/test", "HTTP/1.1", Map.of(), Map.of(), Map.of(), InputStream.nullInputStream(), null );
		return new NGComponent( new NGContext( request ) );
	}

	@Test
	public void checkoutReturnsSavedPage() {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();

		cache.savePage( "1", page, null, null );

		try( NGPageLease lease = cache.checkout( "1" )) {
			assertSame( page, lease.page() );
		}

		// The lock must have been released by close(), so checking the page out again should succeed immediately
		try( NGPageLease lease = cache.checkout( "1" )) {
			assertSame( page, lease.page() );
		}
	}

	@Test
	public void checkoutOfUnknownContextIDThrows() {
		final NGPageCache cache = newCache();
		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "unknown" ) );
	}

	@Test
	public void duplicateContextIDThrows() {
		final NGPageCache cache = newCache();
		cache.savePage( "1", newPage(), null, null );
		assertThrows( IllegalStateException.class, () -> cache.savePage( "1", newPage(), null, null ) );
	}

	@Test
	public void duplicateContextIDThrowsForPartials() {
		final NGPageCache cache = newCache();
		cache.savePage( "root", newPage(), null, null );
		cache.savePage( "frag", newPage(), "root", "container" );
		assertThrows( IllegalStateException.class, () -> cache.savePage( "frag", newPage(), "root", "container" ) );
	}

	@Test
	public void orphanedFragmentSaveIsPromotedToRootEntry() {
		final NGPageCache cache = newCache();

		// The originating context is gone (evicted mid-request) — the save must succeed as a normally evictable root entry,
		// since the action's work is already done and the page is on the user's screen
		cache.savePage( "frag", newPage(), "gone", "container" );

		assertTrue( cache.cacheMap().containsKey( "frag" ) );
		cache.checkout( "frag" ).close();
	}

	@Test
	public void oldestPageGetsEvictedWhenCacheIsFull() {
		final NGPageCache cache = newCache();

		for( int i = 0; i <= PAGE_CACHE_SIZE; i++ ) {
			cache.savePage( String.valueOf( i ), newPage(), null, null );
		}

		assertEquals( PAGE_CACHE_SIZE, cache.cacheMap().size() );
		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "0" ) );
		cache.checkout( "1" ).close();
	}

	@Test
	public void checkoutMovesPageToTopOfCache() {
		final NGPageCache cache = newCache();

		for( int i = 0; i < PAGE_CACHE_SIZE; i++ ) {
			cache.savePage( String.valueOf( i ), newPage(), null, null );
		}

		// Touch the oldest entry, moving it to the top of the cache
		cache.checkout( "0" ).close();

		// The next eviction should now claim "1", while "0" survives
		cache.savePage( "newPage", newPage(), null, null );
		cache.checkout( "0" ).close();
		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "1" ) );
	}

	@Test
	public void checkingOutFragmentRetainsItsContainingPage() {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "root", page, null, null );
		cache.savePage( "frag", page, "root", "container" );

		// Fill the cache to the brim (fragments don't count towards the root cache's size)
		for( int i = 1; i < PAGE_CACHE_SIZE; i++ ) {
			cache.savePage( "filler-" + i, newPage(), null, null );
		}

		// Working with the fragment should move it's containing page to the top of the cache...
		cache.checkout( "frag" ).close();

		// ...so the next eviction claims the oldest filler page instead of the fragment's page
		cache.savePage( "newPage", newPage(), null, null );
		cache.checkout( "root" ).close();
		cache.checkout( "frag" ).close();
		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "filler-1" ) );
	}

	@Test
	public void evictingPageRemovesItsFragments() {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "root", page, null, null );
		cache.savePage( "frag", page, "root", "container" );

		// Push "root" out of the cache
		for( int i = 0; i <= PAGE_CACHE_SIZE; i++ ) {
			cache.savePage( "filler-" + i, newPage(), null, null );
		}

		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "root" ) );
		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "frag" ) );
	}

	@Test
	public void fragmentHistoryIsCappedPerPage() {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "root", page, null, null );

		final int fragmentCount = FRAGMENT_CACHE_SIZE + 10;

		for( int i = 1; i <= fragmentCount; i++ ) {
			cache.savePage( "frag-" + i, page, "root", "container" );
		}

		assertEquals( FRAGMENT_CACHE_SIZE, cache.cacheMap().get( "root" ).children().size() );

		// The oldest fragment entries should have been evicted, the newer ones retained
		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "frag-1" ) );
		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "frag-10" ) );
		cache.checkout( "frag-11" ).close();
		cache.checkout( "frag-" + fragmentCount ).close();
	}

	@Test
	public void cacheEntryMethodsAreSafeOnCyclicStructures() {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "root", page, null, null );
		cache.savePage( "frag", page, "root", "container" );

		final NGPageCacheEntry entry = cache.cacheMap().get( "root" );

		// The record-generated versions of these would throw StackOverflowError on the parent/children cycle
		entry.hashCode();
		entry.toString();

		// Two snapshot copies of the same entry must be equal (identity is the contextID)
		assertEquals( entry, cache.cacheMap().get( "root" ) );
		assertFalse( entry.equals( cache.cacheMap().get( "root" ).children().get( "frag" ) ) );
	}

	@Test
	public void cacheMapReturnsSnapshot() {
		final NGPageCache cache = newCache();
		cache.savePage( "1", newPage(), null, null );

		cache.cacheMap().clear();

		assertEquals( 1, cache.cacheMap().size() );
	}

	@Test
	public void concurrentCheckoutOfSamePageBlocksUntilLeaseIsClosed() throws Exception {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "root", page, null, null );
		cache.savePage( "frag", page, "root", "container" );

		final NGPageLease lease = cache.checkout( "root" );

		final AtomicBoolean secondCheckoutCompleted = new AtomicBoolean();
		final CountDownLatch secondCheckoutStarted = new CountDownLatch( 1 );
		final CountDownLatch secondCheckoutFinished = new CountDownLatch( 1 );

		// A fragment checkout locks it's containing page, so this must block until the first lease is closed
		final Thread thread = new Thread( () -> {
			secondCheckoutStarted.countDown();
			cache.checkout( "frag" ).close();
			secondCheckoutCompleted.set( true );
			secondCheckoutFinished.countDown();
		} );
		thread.start();

		assertTrue( secondCheckoutStarted.await( 5, TimeUnit.SECONDS ) );

		// Give the second checkout a moment to (wrongly) slip through the lock
		Thread.sleep( 200 );
		assertFalse( secondCheckoutCompleted.get() );

		lease.close();

		assertTrue( secondCheckoutFinished.await( 5, TimeUnit.SECONDS ) );
		assertTrue( secondCheckoutCompleted.get() );
	}

	@Test
	public void checkoutsForTheSamePageInstanceSerialize() throws Exception {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();

		// The same page instance saved under a new contextID (e.g. an action returning null/stay-on-page) shares the original entry's lock
		cache.savePage( "1", page, null, null );
		cache.savePage( "2", page, "1", null );

		final NGPageLease lease = cache.checkout( "1" );

		final AtomicBoolean secondCheckoutCompleted = new AtomicBoolean();
		final CountDownLatch secondCheckoutFinished = new CountDownLatch( 1 );

		final Thread thread = new Thread( () -> {
			cache.checkout( "2" ).close();
			secondCheckoutCompleted.set( true );
			secondCheckoutFinished.countDown();
		} );
		thread.start();

		// A checkout of the new context must block while the old context's lease is held — same page instance, same lock
		Thread.sleep( 200 );
		assertFalse( secondCheckoutCompleted.get() );

		lease.close();

		assertTrue( secondCheckoutFinished.await( 5, TimeUnit.SECONDS ) );
		assertTrue( secondCheckoutCompleted.get() );
	}

	@Test
	public void checkoutsSerializeForContextsWithUnrelatedLineage() throws Exception {
		final NGPageCache cache = newCache();
		final NGComponent pageA = newPage();
		final NGComponent pageB = newPage();

		// The "return to a previous page" pattern: A rendered, action on A returned B, action on B returned A again.
		// Contexts "1" and "3" hold the same page instance but share no direct lineage — checkouts must still serialize.
		cache.savePage( "1", pageA, null, null );
		cache.savePage( "2", pageB, "1", null );
		cache.savePage( "3", pageA, "2", null );

		final NGPageLease lease = cache.checkout( "1" );

		final AtomicBoolean secondCheckoutCompleted = new AtomicBoolean();
		final CountDownLatch secondCheckoutFinished = new CountDownLatch( 1 );

		final Thread thread = new Thread( () -> {
			cache.checkout( "3" ).close();
			secondCheckoutCompleted.set( true );
			secondCheckoutFinished.countDown();
		} );
		thread.start();

		Thread.sleep( 200 );
		assertFalse( secondCheckoutCompleted.get() );

		lease.close();

		assertTrue( secondCheckoutFinished.await( 5, TimeUnit.SECONDS ) );
		assertTrue( secondCheckoutCompleted.get() );
	}

	@Test
	public void checkoutsForDifferentPageInstancesRunConcurrently() throws Exception {
		final NGPageCache cache = newCache();
		cache.savePage( "1", newPage(), null, null );
		cache.savePage( "2", newPage(), null, null );

		final NGPageLease lease = cache.checkout( "1" );

		final CountDownLatch secondCheckoutFinished = new CountDownLatch( 1 );

		// Different page instances have different locks — this checkout must NOT wait for the first lease
		final Thread thread = new Thread( () -> {
			cache.checkout( "2" ).close();
			secondCheckoutFinished.countDown();
		} );
		thread.start();

		try {
			assertTrue( secondCheckoutFinished.await( 5, TimeUnit.SECONDS ) );
		}
		finally {
			lease.close();
		}
	}

	@Test
	public void newPageFromContainerActionBecomesRootEntry() {
		final NGPageCache cache = newCache();
		cache.savePage( "1", newPage(), null, null );

		// An action targeting an update container returned a different page instance — it must be stored as a full page, not as a fragment of the old one
		cache.savePage( "2", newPage(), "1", "someContainer" );

		assertTrue( cache.cacheMap().containsKey( "2" ) );
		cache.checkout( "2" ).close();
	}

	@Test
	public void fragmentCapNeverEvictsAContainersNewestFragment() {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "root", page, null, null );

		// A container rendered once...
		cache.savePage( "a1", page, "root", "containerA" );

		// ...must survive any amount of unrelated fragment traffic on the same page (e.g. a polling container)
		for( int i = 1; i <= FRAGMENT_CACHE_SIZE + 10; i++ ) {
			cache.savePage( "b" + i, page, "root", "containerB" );
		}

		cache.checkout( "a1" ).close();
		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "b1" ) );
	}

	@Test
	public void leaseCloseIsIdempotent() {
		final NGPageCache cache = newCache();
		cache.savePage( "1", newPage(), null, null );

		final NGPageLease lease = cache.checkout( "1" );
		lease.close();
		lease.close();

		// The double close must not have released a lock it no longer held — a fresh checkout must still work
		cache.checkout( "1" ).close();
	}

	@Test
	public void chainedFragmentsAllParentToTheRootEntry() {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "root", page, null, null );
		cache.savePage( "f1", page, "root", "container" );

		// A fragment originating from another fragment must still parent directly to the root entry (keeping parent chains one level deep)
		cache.savePage( "f2", page, "f1", "container" );

		final NGPageCacheEntry rootEntry = cache.cacheMap().get( "root" );
		assertEquals( 2, rootEntry.children().size() );
		assertEquals( "root", rootEntry.children().get( "f2" ).parent().contextID() );

		cache.checkout( "f2" ).close();
	}

	@Test
	public void contendedCheckoutThrowsAfterTimeout() throws Exception {
		final NGPageCache cache = new NGPageCache( PAGE_CACHE_SIZE, FRAGMENT_CACHE_SIZE, Duration.ofMillis( 100 ) );
		cache.savePage( "1", newPage(), null, null );

		final NGPageLease lease = cache.checkout( "1" );

		try {
			final AtomicReference<Throwable> thrown = new AtomicReference<>();

			final Thread thread = new Thread( () -> {
				try {
					cache.checkout( "1" );
				}
				catch( Throwable t ) {
					thrown.set( t );
				}
			} );
			thread.start();
			thread.join( 5000 );

			assertInstanceOf( NGPageContendedException.class, thrown.get() );
		}
		finally {
			lease.close();
		}
	}

	@Test
	public void constructorRejectsNonsenseArguments() {
		assertThrows( IllegalArgumentException.class, () -> new NGPageCache( 0, FRAGMENT_CACHE_SIZE, Duration.ofSeconds( 10 ) ) );
		assertThrows( IllegalArgumentException.class, () -> new NGPageCache( PAGE_CACHE_SIZE, 0, Duration.ofSeconds( 10 ) ) );
		assertThrows( IllegalArgumentException.class, () -> new NGPageCache( PAGE_CACHE_SIZE, FRAGMENT_CACHE_SIZE, Duration.ZERO ) );
	}

	@Test
	public void lockSurvivesEvictionAndResave() throws Exception {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "1", page, null, null );

		final NGPageLease lease = cache.checkout( "1" );

		// Evict "1" while it's checked out (eviction is deliberately lock-blind)...
		for( int i = 0; i <= PAGE_CACHE_SIZE; i++ ) {
			cache.savePage( "filler-" + i, newPage(), null, null );
		}

		assertThrows( NGPageRestorationException.class, () -> cache.checkout( "1" ) );

		// ...then save the same instance under a new context (it's still alive, on the user's screen)
		cache.savePage( "new", page, null, null );

		// The lock belongs to the page instance, not to it's cache entries — checkout of the new context must still block
		final AtomicBoolean completed = new AtomicBoolean();
		final CountDownLatch finished = new CountDownLatch( 1 );

		final Thread thread = new Thread( () -> {
			cache.checkout( "new" ).close();
			completed.set( true );
			finished.countDown();
		} );
		thread.start();

		Thread.sleep( 200 );
		assertFalse( completed.get() );

		lease.close();

		assertTrue( finished.await( 5, TimeUnit.SECONDS ) );
		assertTrue( completed.get() );
	}

	@Test
	public void lockPageSerializesWithCheckoutOfTheSameInstance() throws Exception {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "1", page, null, null );

		final NGPageLease lease = cache.checkout( "1" );

		// A request rendering this instance as it's response page (without having checked it out) must wait for the lease
		final AtomicBoolean completed = new AtomicBoolean();
		final CountDownLatch finished = new CountDownLatch( 1 );

		final Thread thread = new Thread( () -> {
			cache.lockPage( page ).close();
			completed.set( true );
			finished.countDown();
		} );
		thread.start();

		Thread.sleep( 200 );
		assertFalse( completed.get() );

		lease.close();

		assertTrue( finished.await( 5, TimeUnit.SECONDS ) );
	}

	@Test
	public void lockPageIsReentrantWithinTheHoldingThread() throws Exception {
		final NGPageCache cache = newCache();
		final NGComponent page = newPage();
		cache.savePage( "1", page, null, null );

		try( NGPageLease lease = cache.checkout( "1" )) {

			// The response page may be the checked out page itself — re-acquisition on the same thread must succeed immediately
			try( NGPageLease renderLease = cache.lockPage( page )) {
				assertSame( page, renderLease.page() );
			}

			// Closing the inner lease must not have released the outer one — another thread must still block
			final AtomicBoolean completed = new AtomicBoolean();

			final Thread thread = new Thread( () -> {
				try {
					cache.lockPage( page ).close();
					completed.set( true );
				}
				catch( NGPageContendedException e ) {
					// Expected: the test ends by interrupting this thread while it's waiting for the lock
				}
			} );
			thread.start();

			Thread.sleep( 200 );
			assertFalse( completed.get() );

			thread.interrupt();
			thread.join( 5000 );
		}
	}
}
