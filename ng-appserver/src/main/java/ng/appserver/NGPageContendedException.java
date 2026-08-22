package ng.appserver;

import java.time.Duration;

/**
 * Thrown when acquiring a page's lock times out, i.e. a previous request working with the same page
 * hasn't finished (probably stuck in a long running action).
 */
public class NGPageContendedException extends RuntimeException {

	/**
	 * The lock timeout that expired. Carried along so response generation can derive an honest Retry-After hint.
	 */
	private final Duration _lockTimeout;

	public NGPageContendedException( String message, Duration lockTimeout ) {
		super( message );
		_lockTimeout = lockTimeout;
	}

	public NGPageContendedException( String message, Duration lockTimeout, Throwable cause ) {
		super( message, cause );
		_lockTimeout = lockTimeout;
	}

	public Duration lockTimeout() {
		return _lockTimeout;
	}
}
