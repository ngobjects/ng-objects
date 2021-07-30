package ng.appserver;

/**
 * Yes, we have sessions too!
 */

public class NGSession {

	final String _sessionID;
	final long _birthDate;

	public NGSession( final String sessionID ) {
		_sessionID = sessionID;
		_birthDate = System.currentTimeMillis();
	}
}