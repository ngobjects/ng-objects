package ng.appserver;

import java.util.List;

/**
 * Defines the basic methods required for a session store implementation
 */

public abstract class NGSessionStore {

	public abstract NGSession checkoutSessionWithID( final String sessionID );

	public abstract void storeSession( final NGSession session );

	/**
	 * @return All sessions stored by this session store.
	 */
	public abstract List<NGSession> sessions();
}