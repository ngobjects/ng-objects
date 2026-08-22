package ng.appserver;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import ng.appserver.templating.NGComponent;

/**
 * Test harness for driving NGComponentRequestHandler end to end — request in, response out — without booting
 * an application, an adaptor or the session store.
 *
 * The pieces:
 *
 * - TestPage: a template-less page whose three phases are observable and scriptable (set actionHandler/renderCallback, read the invoked-flags)
 * - TestRequest: an NGStandardRequest wired directly to a given NGSession, bypassing cookie/session store resolution
 * - handle(): constructs the request + context pair and pushes it through a fresh NGComponentRequestHandler
 *
 * Typical use: create an NGSession, save a TestPage in it's page cache under a contextID, then
 * handle( session, "/no/&lt;contextID&gt;.0", Map.of() ) and assert on the response, the page's flags and the cache's state.
 */
public class NGComponentRequestTestHarness {

	/**
	 * A page whose phase methods are scriptable and observable, and whose rendering requires no template:
	 * appendOrTraverse() appends the page's marker string, so a response's content identifies which page rendered it.
	 */
	public static class TestPage extends NGComponent {

		/**
		 * Invoked by invokeAction(). Defaults to returning null ("stay on the same page"). Set to script the action's result — or make it throw.
		 */
		public Function<NGContext, NGActionResults> actionHandler = context -> null;

		/**
		 * Invoked at the start of rendering. Defaults to a no-op. Set to observe/block/fail the render phase.
		 */
		public Runnable renderCallback = () -> {};

		public boolean takeValuesInvoked;
		public boolean actionInvoked;

		private final String _marker;

		public TestPage( final NGContext context, final String marker ) {
			super( context );
			_marker = marker;
		}

		@Override
		public void takeValuesFromRequest( NGRequest request, NGContext context ) {
			takeValuesInvoked = true;
		}

		@Override
		public NGActionResults invokeAction( NGRequest request, NGContext context ) {
			actionInvoked = true;
			return actionHandler.apply( context );
		}

		@Override
		public void appendOrTraverse( NGResponse response, NGContext context ) {
			renderCallback.run();
			response.appendContentString( _marker );
		}
	}

	/**
	 * A request bound directly to the given session, so request handling never touches cookies or the session store.
	 */
	public static class TestRequest extends NGStandardRequest {

		private final NGSession _testSession;

		public TestRequest( final String uri, final NGSession session, final Map<String, List<String>> formValues ) {
			super( "GET", uri, "HTTP/1.1", Map.of(), formValues, Map.of(), InputStream.nullInputStream(), null );
			_testSession = session;
		}

		@Override
		public NGSession session() {
			return _testSession;
		}

		@Override
		public NGSession existingSession() {
			return _testSession;
		}

		@Override
		public boolean hasSession() {
			return true;
		}
	}

	/**
	 * @return A new TestPage rendering the given marker string, born with a throwaway context (request handling replaces a page's context anyway)
	 */
	public static TestPage newPage( final String marker ) {
		final TestRequest birthRequest = new TestRequest( "/no/birth", new NGSession(), Map.of() );
		return new TestPage( new NGContext( birthRequest ), marker );
	}

	/**
	 * Pushes a component action request for the given URI through NGComponentRequestHandler against the given session.
	 *
	 * @param session The session whose page cache the request works with
	 * @param uri A component action URI, e.g. "/no/1.0" (contextID 1, senderID 0)
	 * @param formValues The request's form values (an empty map means the takeValuesFromRequest phase is skipped)
	 */
	public static NGResponse handle( final NGSession session, final String uri, final Map<String, List<String>> formValues ) {
		final TestRequest request = new TestRequest( uri, session, formValues );

		// The context registers itself with the request (request.setContext) in it's constructor
		new NGContext( request );

		return new NGComponentRequestHandler().handleRequest( request );
	}
}
