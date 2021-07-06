package ng.appserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default sessionstore
 */

public class NGServerSessionStore extends NGSessionStore {

	final Map<String,NGSession> _sessions = new ConcurrentHashMap<>();
	
	public NGSession checkoutSessionWithID( final String sessionID ) {
		return _sessions.get( sessionID );
	}
}