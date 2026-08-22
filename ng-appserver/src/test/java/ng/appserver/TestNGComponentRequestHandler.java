package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import ng.appserver.NGComponentRequestTestHarness.TestPage;

/**
 * Drives NGComponentRequestHandler end to end through NGComponentRequestTestHarness,
 * primarily to pin down the page locking rules that only exist at the handler level.
 */
public class TestNGComponentRequestHandler {

	@Test
	public void actionReturningNullRerendersTheCheckedOutPage() {
		final NGSession session = new NGSession();
		final TestPage page = NGComponentRequestTestHarness.newPage( "PAGE_A" );
		session.pageCache().savePage( "1", page, null, null );

		final NGResponse response = NGComponentRequestTestHarness.handle( session, "/no/1.0", Map.of() );

		assertTrue( page.actionInvoked );
		assertEquals( "PAGE_A", response.contentString() );

		// The lease must have been released once the request was handled
		session.pageCache().lockPage( page ).close();
	}

	@Test
	public void takeValuesRunsOnlyWhenFormValuesArePresent() {
		final NGSession session = new NGSession();
		final TestPage page = NGComponentRequestTestHarness.newPage( "PAGE_A" );
		session.pageCache().savePage( "1", page, null, null );

		NGComponentRequestTestHarness.handle( session, "/no/1.0", Map.of() );
		assertFalse( page.takeValuesInvoked );

		NGComponentRequestTestHarness.handle( session, "/no/1.0", Map.of( "someField", List.of( "someValue" ) ) );
		assertTrue( page.takeValuesInvoked );
	}

	@Test
	public void leaseIsReleasedWhenTheActionThrows() {
		final NGSession session = new NGSession();
		final TestPage page = NGComponentRequestTestHarness.newPage( "PAGE_A" );
		session.pageCache().savePage( "1", page, null, null );

		page.actionHandler = context -> {
			throw new IllegalStateException( "BOOM" );
		};

		assertThrows( IllegalStateException.class, () -> NGComponentRequestTestHarness.handle( session, "/no/1.0", Map.of() ) );

		// The try-with-resources in the handler must have released the lease despite the throw
		session.pageCache().lockPage( page ).close();
	}

	@Test
	public void leaseIsReleasedWhenRenderingThrows() {
		final NGSession session = new NGSession();
		final TestPage page = NGComponentRequestTestHarness.newPage( "PAGE_A" );
		session.pageCache().savePage( "1", page, null, null );

		page.renderCallback = () -> {
			throw new RuntimeException( "RENDERBOOM" );
		};

		assertThrows( RuntimeException.class, () -> NGComponentRequestTestHarness.handle( session, "/no/1.0", Map.of() ) );

		session.pageCache().lockPage( page ).close();
	}

	@Test
	public void returnedPageIsRenderedUnderItsOwnLock() throws Exception {
		final NGSession session = new NGSession();
		final TestPage pageA = NGComponentRequestTestHarness.newPage( "PAGE_A" );
		final TestPage pageB = NGComponentRequestTestHarness.newPage( "PAGE_B" );
		session.pageCache().savePage( "1", pageA, null, null );
		session.pageCache().savePage( "2", pageB, null, null );

		// The action on A returns B — a cached instance another request could be working with
		pageA.actionHandler = context -> pageB;

		final CountDownLatch renderStarted = new CountDownLatch( 1 );
		final CountDownLatch renderMayFinish = new CountDownLatch( 1 );

		pageB.renderCallback = () -> {
			renderStarted.countDown();
			try {
				renderMayFinish.await();
			}
			catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
				throw new RuntimeException( e );
			}
		};

		final AtomicReference<NGResponse> response = new AtomicReference<>();

		final Thread handlerThread = new Thread( () -> response.set( NGComponentRequestTestHarness.handle( session, "/no/1.0", Map.of() ) ) );
		handlerThread.start();

		assertTrue( renderStarted.await( 5, TimeUnit.SECONDS ) );

		// While the handler renders B, another thread must not be able to acquire B's lock
		final AtomicBoolean lockedDuringRender = new AtomicBoolean();

		final Thread contender = new Thread( () -> {
			session.pageCache().lockPage( pageB ).close();
			lockedDuringRender.set( true );
		} );
		contender.start();

		Thread.sleep( 200 );
		assertFalse( lockedDuringRender.get() );

		renderMayFinish.countDown();
		handlerThread.join( 5000 );
		contender.join( 5000 );

		assertTrue( lockedDuringRender.get() );
		assertEquals( "PAGE_B", response.get().contentString() );

		// Both A's and B's locks must be free once the request is handled
		session.pageCache().lockPage( pageA ).close();
		session.pageCache().lockPage( pageB ).close();
	}
}
