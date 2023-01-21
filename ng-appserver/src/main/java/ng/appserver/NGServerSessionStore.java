package ng.appserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default session store
 *
 * FIXME: Implement session timeouts
 */

public class NGServerSessionStore extends NGSessionStore {

	private static final Logger logger = LoggerFactory.getLogger( NGServerSessionStore.class );

	final Map<String, NGSession> _sessions = new ConcurrentHashMap<>();

	public NGServerSessionStore() {
		final TimerTask sessionKillerTask = new TimerTask() {
			@Override
			public void run() {
				logger.debug( "Harvesting dead sessions" ); // FIXME: This logging is a little much, just here for the development stage // Hugi 2023-01-21

				// FIXME: This is, of course, horribly inefficient // Hugi 2023-01-21
				for( NGSession session : sessions() ) {
					if( session.shouldTerminate() ) {
						_sessions.remove( session.sessionID() );
					}
				}
			}
		};

		final Timer timer = new Timer( "Sessionkiller" );
		timer.schedule( sessionKillerTask, 5000, 5000 ); // FIXME: execution times might need to be tuned a little // Hugi 2023-01-21
	}

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