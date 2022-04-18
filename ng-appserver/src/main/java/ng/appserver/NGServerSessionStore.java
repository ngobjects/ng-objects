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

	@Override
	public NGSession checkoutSessionWithID( final String sessionID ) {
		return _sessions.get( sessionID );
	}
}