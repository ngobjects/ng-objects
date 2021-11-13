package ng.appserver;

import java.util.UUID;

/**
 * Yes, we have sessions too!
 */

public class NGSession {

	final String _sessionID;
	final long _birthDate;

	public NGSession() {
		this( UUID.randomUUID().toString() );
	}

	public NGSession( final String sessionID ) {
		this( sessionID, System.currentTimeMillis() );
	}

	private NGSession( final String sessionID, long birthDate ) {
		_sessionID = sessionID;
		_birthDate = birthDate;
	}
}