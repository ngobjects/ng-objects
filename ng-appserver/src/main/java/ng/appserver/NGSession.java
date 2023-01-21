package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
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
	 * FIXME: Use an Instant instead? // Hugi 2023-01-11
	 * FIXME: Calling an instant a "date" is not nice // Hugi 2023-01-21
	 */
	private final long _birthDate;

	/**
	 * The last date at which this session was touched
	 */
	private long _lastTouchedDate;

	/**
	 * FIXME: Use seconds instead? Millisecond sessions might be something of an overreach. Duration could also be nice. // Hugi 2023-01-21
	 */
	private long _timeOutInMilliseconds;

	/**
	 * Boolean set by terminate() to indicate that this session should be terminated, regardless of it's timeut
	 */
	private boolean _manuallyTerminated = false;

	/**
	 * FIXME: This is not the way we're going to store/cache contexts. Just for testing
	 * FIXME: I don't think we actually need this? Isn't the page cache really the only thing we need for context storage?
	 */
	//	public final List<NGContext> contexts = new ArrayList<>();

	/**
	 * Holds our context ID
	 *
	 * FIXME: Only public while we're testing this implementation // Hugi 2023-01-21
	 * FIXME: At what point do we reset the counter? // Hugi 2023-01-21
	 */
	private int currentContextID = 0;

	/**
	 * The page cache is going to have to keep track of
	 *
	 *  1. The originating context ID
	 *  2. The elementID the page originates from (for example, the click of a link)
	 *
	 *  So, let's just for now store the page as an accumulation of the entire string after the request handler key
	 *
	 * FIXME: While a session would usually only be working with one page at a time, this might have to be looked into WRT concurrency? // Hugi 2023-01-21
	 */
	private Map<String, NGComponent> _pageCache = new HashMap<>();

	public NGSession() {
		this( UUID.randomUUID().toString() );
	}

	private NGSession( final String sessionID ) {
		this( sessionID, System.currentTimeMillis() );
	}

	private NGSession( final String sessionID, long birthDate ) {
		_sessionID = sessionID;
		_birthDate = birthDate;
		_lastTouchedDate = birthDate;
		//		_timeOutInMilliseconds = 60 * 60 * 1000; // FIXME: Default timeout of one hour. Should be set, probably somewhere in NGApplication (and/or using a property)
		_timeOutInMilliseconds = Duration.ofMinutes( 1 ).toMillis(); // FIXME: Default timeout of one hour. Should be set, probably somewhere in NGApplication (and/or using a property)
	}

	public int getContextIDAndIncrement() {
		int contextID = currentContextID++;
		return contextID;
	}

	public String sessionID() {
		return _sessionID;
	}

	/**
	 * @return The time at which this session was created
	 *
	 * FIXME: Are we surewe want this as an instant? // Hugi 2023-01-21
	 * FIXME: Don't really like the name, which implements a "date", while it's really an instant // Hugi 2023-01-21
	 */
	public Instant birthDate() {
		return Instant.ofEpochMilli( _birthDate );
	}

	/**
	 * @return The last date at which this session was touched
	 */
	public Instant lastTouchedDate() {
		return Instant.ofEpochMilli( _lastTouchedDate );
	}

	/**
	 * "Touches" the session, indicating that it has been used (and thus prolonging it's life)
	 */
	public void touch() {

	}

	/**
	 * @return The session's timeout in milliseconds, i.e. the time the session will live after last being touched.
	 *
	 * FIXME: Are we sure we want milliseconds? A java.time.Duration feels like it would be excellent here // Hugi 2023-01-21
	 */
	public long timeoutInMilliseconds() {
		return _timeOutInMilliseconds;
	}

	/**
	 * @return true if the session has timed out (and is thus due to be harvested/erased
	 */
	private boolean isTimedOut() {
		return lastTouchedDate().plusMillis( timeoutInMilliseconds() ).isBefore( Instant.now() );
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
	 * FIXME: This is horrid and does not belong here // Hugi 2022-06-25
	 */
	public static NGSession createSession() {
		try {
			return NGApplication.application()._sessionClass().getConstructor().newInstance();
		}
		catch( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			throw new RuntimeException( e );
		}
	}

	public void savePage( String contextID, NGComponent component ) {
		logger.debug( "Saving page {} in cache with contextID {} ", component.getClass(), contextID );
		_pageCache.put( contextID, component );
	}

	public NGComponent restorePageFromCache( String contextID ) {
		logger.debug( "Restoring page from cache with contextID: " + contextID );
		return _pageCache.get( contextID );
	}
}