package ng.appserver;

public abstract class NGSessionStore {

	public abstract NGSession checkoutSessionWithID( final String sessionID );
}