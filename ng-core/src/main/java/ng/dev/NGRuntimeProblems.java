package ng.dev;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A bounded, in-memory record of the runtime problems an application rendered into its pages —
 * primarily template binding errors (the inline error boxes both frameworks draw when a binding
 * fails: ng-objects' NGErrorMessageElement and Parsley's ParsleyErrorMessageElement).
 *
 * Why this exists: those error boxes are visible to a human looking at the page, but an external
 * tool (a script, or an AI agent driving the app) only sees them by scraping rendered HTML. This
 * buffer makes them one HTTP fetch away instead (the /problems dev endpoints in both frameworks),
 * the same trick NGConsoleCapture plays for console output.
 *
 * Lives in ng-core so both stacks share one store and one behavior: each framework's error
 * element calls {@link #record} at its render choke point, and each framework's dev endpoint
 * reads {@link #snapshot} — same shapes, no drift.
 */
public class NGRuntimeProblems {

	/**
	 * A single recorded problem.
	 *
	 * @param epochMillis when the problem was rendered
	 * @param kind the problem category, e.g. "Unknown key" or "Binding configuration error"
	 * @param element the element/component involved, when known (may be empty)
	 * @param message the human-readable problem message
	 */
	public record Problem( long epochMillis, String kind, String element, String message ) {}

	/**
	 * Most recent problems retained. Bounded so memory stays flat over long sessions.
	 */
	private static final int MAX_PROBLEMS = 1000;

	/**
	 * The ring buffer of recent problems. Guarded by itself.
	 */
	private static final Deque<Problem> _problems = new ArrayDeque<>( MAX_PROBLEMS );

	private NGRuntimeProblems() {}

	/**
	 * Records a rendered problem. Called from the error elements' appendToResponse, so a problem
	 * that renders on every request is recorded on every request — that repetition is signal
	 * (it tells the reader the problem is still live), and the bounded buffer keeps it safe.
	 */
	public static void record( final String kind, final String element, final String message ) {

		final Problem problem = new Problem( System.currentTimeMillis(), nonNull( kind ), nonNull( element ), nonNull( message ) );

		synchronized( _problems ) {
			if( _problems.size() == MAX_PROBLEMS ) {
				_problems.removeFirst();
			}
			_problems.addLast( problem );
		}
	}

	/**
	 * @param contains if non-null, only problems whose kind, element or message contains the given string (case-sensitive) are returned
	 * @param tail if > 0, at most that many problems are returned, preferring the most recent
	 * @return the matching problems, oldest first
	 */
	public static List<Problem> snapshot( final String contains, final int tail ) {

		List<Problem> result;

		synchronized( _problems ) {
			result = new ArrayList<>( _problems );
		}

		if( contains != null && !contains.isEmpty() ) {
			result = result.stream()
					.filter( p -> p.kind().contains( contains ) || p.element().contains( contains ) || p.message().contains( contains ) )
					.toList();
		}

		if( tail > 0 && result.size() > tail ) {
			result = result.subList( result.size() - tail, result.size() );
		}

		return result;
	}

	/**
	 * Empties the buffer. Exposed through the dev endpoints ( clear=true ) so a tool can mark a
	 * clean baseline before exercising the app, then read back only what that exercise produced.
	 */
	public static void clear() {

		synchronized( _problems ) {
			_problems.clear();
		}
	}

	/**
	 * @return the number of problems currently retained
	 */
	public static int size() {

		synchronized( _problems ) {
			return _problems.size();
		}
	}

	private static String nonNull( final String value ) {
		return value != null ? value : "";
	}
}
