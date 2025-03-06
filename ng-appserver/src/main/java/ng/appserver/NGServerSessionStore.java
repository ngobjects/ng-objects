package ng.appserver;

import java.time.Duration;
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
 */

public class NGServerSessionStore extends NGSessionStore {

	private static final Logger logger = LoggerFactory.getLogger( NGServerSessionStore.class );

	final Map<String, NGSession> _sessions = new ConcurrentHashMap<>();

	public NGServerSessionStore() {
		startExpiredSessionReaperThread();
	}

	/**
	 * Initialize a thread that will remove expired sessions
	 */
	private void startExpiredSessionReaperThread() {
		final TimerTask sessionReaperTask = new TimerTask() {
			@Override
			public void run() {

				// CHECKME: Iterating through every session every five seconds itsn't exactly efficient. We might want to look into some alternative methods // Hugi 2023-01-21
				for( final NGSession session : sessions() ) {
					if( session.shouldTerminate() ) {
						_sessions.remove( session.sessionID() );
						logger.debug( "Terminated session with ID {}", session.sessionID() );
					}
				}
			}
		};

		final Timer timer = new Timer( "SessionReaper", true );
		final long timeBeforeFirstExecution = Duration.ofSeconds( 5 ).toMillis();
		final long timeBetweenExecutions = Duration.ofSeconds( 5 ).toMillis();
		timer.schedule( sessionReaperTask, timeBeforeFirstExecution, timeBetweenExecutions ); // CHECKME: The execution times might need configuration // Hugi 2023-01-21
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