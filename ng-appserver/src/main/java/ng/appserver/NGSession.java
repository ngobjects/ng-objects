package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	 * FIXME: This is not the way we're going to store/cache contexts. Just for testing
	 */
	public List<NGContext> contexts = new ArrayList<>();

	/**
	 * A unique identifier for this session
	 */
	private final String _sessionID;

	/**
	 * The birthdate of this session, as provided by System.currentTimeMillis()
	 *
	 * FIXME: Use an Instant instead? // Hugi 2023-01-11
	 */
	private final long _birthDate;

	/**
	 * FIXME: Use seconds instead?
	 */
	private long _timeOutInMilliseconds;

	/**
	 * FIXME: OK, this is horrible, but we're going to start out with out pageCache here. This belongs in the session, really.
	 *
	 * The page cache is going to have to keep track of
	 *
	 *  1. The originating context ID
	 *  2. The elementID the page originates from (for example, the click of a link)
	 *
	 *  So, let's just for now store the page as an accumulation of the entire string after the request handler key
	 */
	public Map<String, NGComponent> _pageCache = new HashMap<>();

	public NGSession() {
		this( UUID.randomUUID().toString() );
	}

	private NGSession( final String sessionID ) {
		this( sessionID, System.currentTimeMillis() );
	}

	private NGSession( final String sessionID, long birthDate ) {
		_sessionID = sessionID;
		_birthDate = birthDate;
		_timeOutInMilliseconds = 60 * 60 * 1000; // Default timeout of one hour
	}

	public String sessionID() {
		return _sessionID;
	}

	public Instant birthDate() {
		return Instant.ofEpochMilli( _birthDate );
	}

	public long timeoutInMilliseconds() {
		return _timeOutInMilliseconds;
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