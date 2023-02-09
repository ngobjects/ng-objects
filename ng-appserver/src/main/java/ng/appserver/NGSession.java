package ng.appserver;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yes, we have sessions too!
 */

public class NGSession {

	private static final Logger logger = LoggerFactory.getLogger( NGSession.class );

	/**
	 * A unique identifier for this session
	 */
	private final String _sessionID;

	/**
	 * The birthdate of this session, as provided by System.currentTimeMillis()
	 *
	 * FIXME: Calling an instant a "date" is not nice // Hugi 2023-01-21
	 */
	private final Instant _birthDate;

	/**
	 * The last date at which this session was touched
	 *
	 * FIXME: Calling an instant a "date" is not nice // Hugi 2023-01-21
	 */
	private Instant _lastTouchedDate;

	/**
	 * The session's timeout
	 */
	private Duration _timeOut;

	/**
	 * Boolean set by terminate() to indicate that this session should be terminated, regardless of it's timeout
	 */
	private boolean _manuallyTerminated = false;

	/**
	 * Holds our context ID
	 */
	private int currentContextID = 0;

	/**
	 * In the case of component actions, stores the currently active page instance by contextID.
	 *
	 * FIXME: We're currently storing every page forever. The size of the cache needs to be limited // Hugi 2023-01-21
	 */
	public Map<String, NGComponent> _pageCache = new LinkedHashMap<>();

	public NGSession() {
		this( UUID.randomUUID().toString() );
	}

	private NGSession( final String sessionID ) {
		this( sessionID, Instant.now() );
	}

	/**
	 * @return THe size of the page cache
	 *
	 * FIXME: Temporary location for this parameter, will eventually be loaded from Properties
	 */
	private int pageCacheSize() {
		return 10;
	}

	private NGSession( final String sessionID, final Instant birthDate ) {
		_sessionID = sessionID;
		_birthDate = birthDate;
		_lastTouchedDate = birthDate;
		_timeOut = defaultTimeOut();
	}

	/**
	 * @return The current contextID, incrementing our context counter for next invocation. Used by NGContext to get a unique context ID.
	 */
	public int getContextIDAndIncrement() {
		int contextID = currentContextID++;
		return contextID;
	}

	/**
	 * @return This session's ID (stored in a cookie on the client browser to identify the session)
	 */
	public String sessionID() {
		return _sessionID;
	}

	/**
	 * @return The time at which this session was created
	 */
	public Instant birthDate() {
		return _birthDate;
	}

	/**
	 * @return The last date at which this session was touched
	 */
	public Instant lastTouchedDate() {
		return _lastTouchedDate;
	}

	/**
	 * "Touches" the session, indicating that it has been used (and thus prolonging it's life)
	 */
	public void touch() {
		_lastTouchedDate = Instant.now();
	}

	/**
	 * @return The session's timeout in milliseconds, i.e. the time the session will live after last being touched.
	 */
	public Duration timeOut() {
		return _timeOut;
	}

	/**
	 * @return The default timeout for a session
	 *
	 * FIXME: Default timeout of one hour. Should be configurable by the user, probably in NGApplication (and/or using a property) // Hugi 2023-01-21
	 */
	private static Duration defaultTimeOut() {
		return Duration.ofMinutes( 60 );
	}

	/**
	 * @return true if the session has timed out (and is thus due to be harvested/erased
	 */
	private boolean isTimedOut() {
		return lastTouchedDate().plus( timeOut() ).isBefore( Instant.now() );
	}

	/**
	 * @return true if this session should be harvested/terminated by session storage. This essentially means the session has timed out, or has been manually terminated
	 */
	public boolean shouldTerminate() {
		return _manuallyTerminated || isTimedOut();
	}

	/**
	 * Terminates the session and removes it from it's storage.
	 *
	 * FIXME: We need to notify the session's storage that the session has been terminated // Hugi 2023-01-21
	 * FIXME: Should this method trigger the deletion of the session cookie as well? // Hugi 2023-01-21
	 */
	public void terminate() {
		_manuallyTerminated = true;
	}

	/**
	 * Saves the given page in the page cache
	 */
	public void savePage( final String contextID, final NGComponent component ) {
		logger.debug( "Saving page {} in cache with contextID {} ", component.getClass(), contextID );
		_pageCache.put( contextID, component );
	}

	/**
	 * Retrieves the page with the given contextID from the page cache
	 */
	public NGComponent restorePageFromCache( final String contextID ) {
		logger.debug( "Restoring page from cache with contextID: " + contextID );
		return _pageCache.get( contextID );
	}

	/**
	 * Moves the given page to the front of the page cache
	 */
	public void retainPageWithContextIDInCache( final String contextID ) {
		logger.debug( "Retaining contextID {} in cache", contextID );
		final NGComponent component = _pageCache.remove( contextID );
		_pageCache.put( contextID, component );
	}
}