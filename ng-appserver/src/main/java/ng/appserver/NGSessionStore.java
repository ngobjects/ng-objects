package ng.appserver;

import java.util.List;

/**
 * FIXME: Should the session store perhaps be an interface? Not a lot of logic here at the moment // Hugi 2023-01-21
 */

public abstract class NGSessionStore {

	public abstract NGSession checkoutSessionWithID( final String sessionID );

	public abstract void storeSession( final NGSession session );

	/**
	 * @return All sessions stored by this session store.
	 */
	public abstract List<NGSession> sessions();
}