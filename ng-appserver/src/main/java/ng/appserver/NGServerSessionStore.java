package ng.appserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default session store
 *
 * FIXME: Implement session timeouts
 */

public class NGServerSessionStore extends NGSessionStore {

	final Map<String, NGSession> _sessions = new ConcurrentHashMap<>();

	private static NGSession _fakeSession;

	/**
	 * FIXME: We might want to implement this someday. Really.
	 */
	@Override
	public NGSession checkoutSessionWithID( final String sessionID ) {
		if( _fakeSession == null ) {
			_fakeSession = NGSession.createSession();
		}

		return _fakeSession;
		//		final NGSession existingSession = _sessions.get( sessionID );
		//
		//		if( existingSession == null ) {
		//
		//		}
		//
		//		return existingSession;
	}
}