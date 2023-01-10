package ng.appserver;

public abstract class NGSessionStore {

	public abstract NGSession checkoutSessionWithID( final String sessionID );

	public abstract void storeSession( final NGSession session );
}