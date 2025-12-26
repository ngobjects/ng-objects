package ng.appserver;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Yes, we have sessions too!
 */

public class NGSession {

	/**
	 * A unique identifier for this session
	 */
	private final String _sessionID;

	/**
	 * The birth date of this session
	 */
	private final Instant _birthDate;

	/**
	 * The time at which this session was last touched
	 */
	private Instant _lastTouched;

	/**
	 * The session's timeout
	 */
	private Duration _timeOut;

	/**
	 * Boolean set by terminate() to indicate that this session should be terminated, regardless of it's timeout
	 */
	private boolean _manuallyTerminated = false;

	/**
	 * ID of the context last rendered by this session
	 */
	private int currentContextID = 0;

	/**
	 * In the case of component actions, stores the currently active page instance by contextID.
	 */
	private NGPageCache _pageCache = new NGPageCache();

	public NGSession() {
		this( UUID.randomUUID().toString() );
	}

	private NGSession( final String sessionID ) {
		this( sessionID, Instant.now() );
	}

	private NGSession( final String sessionID, final Instant birthDate ) {
		_sessionID = sessionID;
		_birthDate = birthDate;
		_lastTouched = birthDate;
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
	public Instant lastTouched() {
		return _lastTouched;
	}

	/**
	 * "Touches" the session, indicating that it has been used (and thus prolonging it's life)
	 */
	public void touch() {
		_lastTouched = Instant.now();
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
		return lastTouched().plus( timeOut() ).isBefore( Instant.now() );
	}

	/**
	 * @return true if this session should be harvested/terminated by session storage. This essentially means the session has timed out, or has been manually terminated
	 */
	public boolean shouldReap() {
		return _manuallyTerminated || isTimedOut();
	}

	/**
	 * Terminates the session and removes it from it's storage.
	 */
	public void terminate() {
		_manuallyTerminated = true;
	}

	/**
	 * @return The session's page cache
	 */
	public NGPageCache pageCache() {
		return _pageCache;
	}
}