package ng.appserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default session store
 *
 * FIXME: Implement session timeouts
 */

public class NGServerSessionStore extends NGSessionStore {

	final Map<String, NGSession> _sessions = new ConcurrentHashMap<>();

	/**
	 * FIXME: We might want to implement this someday. Really.
	 */
	@Override
	public NGSession checkoutSessionWithID( final String sessionID ) {
		return _sessions.get( sessionID );
	}

	@Override
	public void storeSession( NGSession session ) {
		_sessions.put( session.sessionID(), session );
	}

	@Override
	public List<NGSession> sessions() {
		return new ArrayList<>( _sessions.values() );
	}
}