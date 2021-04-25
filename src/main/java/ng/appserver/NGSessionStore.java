package ng.appserver;

import java.util.HashMap;
import java.util.Map;

/**
 * FIXME: This should be an abstract class or an interface. We want persistent sessions at some point.
 * FIXME: Actually implement  
 */

public class NGSessionStore {

	Map<String,NGSession> _sessions = new HashMap<>();
	
	public NGSession checkoutSessionWithID( final String sessionID ) {
		return _sessions.get( sessionID );
	}
}