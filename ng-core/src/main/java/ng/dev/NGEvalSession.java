package ng.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.SourceCodeAnalysis.CompletionInfo;
import jdk.jshell.UnresolvedReferenceException;
import jdk.jshell.execution.LocalExecutionControlProvider;

/**
 * A persistent, in-process Java evaluation session — the engine behind the /eval development
 * endpoints in both frameworks (ng-objects' /ng/dev/eval and wonder-slim's .woa/eval).
 *
 * Why this exists: an external tool (a script, or an AI agent driving a running app) frequently
 * needs to ask the app a question its HTTP surface can't answer — "what does this object look
 * like", "what does this method return for real data". Without this, that means a separate jshell
 * process against the compiled classes: no live application state, no shared statics, second-rate.
 * This session evaluates snippets <em>inside the running JVM</em> instead.
 *
 * How the live-state magic works: JShell normally spawns a separate execution JVM. We use
 * {@link LocalExecutionControlProvider}, which executes snippets in the current JVM, in a class
 * loader that parent-delegates to the application's own loader — so a snippet that calls
 * {@code MyApp.someStatic()} sees the exact same class, same statics, same live singletons as
 * the application code. (Assumption: the app runs with its classes on the regular launch
 * classpath — true for standard {@code java -cp} launches, including Eclipse-launched apps.
 * The compilation classpath is seeded from {@code java.class.path}.)
 *
 * The session is persistent: variables and definitions survive across evaluations, so a tool can
 * build up state interactively ({@code var ctx = ...} then use {@code ctx} in the next call).
 * {@link #reset()} discards everything and starts fresh.
 *
 * Printed output (System.out/err) intentionally goes to the app's console — where the console
 * capture makes it readable via the /log dev endpoints — rather than being captured per-snippet;
 * the response carries the <em>value</em> of the evaluation, diagnostics and any exception.
 *
 * Known limitation, deliberate for a dev-only tool: a snippet that never returns (an infinite
 * loop) blocks its request thread; there is no safe way to kill a thread in-process. Restart the
 * app if you manage to hang it.
 */
public class NGEvalSession {

	/**
	 * The shared session used by the dev endpoints — one persistent session per JVM, matching
	 * "one app per JVM". Guarded by the class monitor during lazy creation.
	 */
	private static NGEvalSession _shared;

	/**
	 * The underlying JShell. Created lazily (JShell startup isn't free, and most app runs never
	 * touch /eval); replaced wholesale by {@link #reset()}.
	 */
	private JShell _shell;

	/**
	 * The result of evaluating an input.
	 *
	 * @param ok true when every snippet compiled and ran without throwing
	 * @param value the value of the last evaluated snippet, or null when there was none (statements, definitions)
	 * @param exception a description of the thrown exception, or null when nothing threw
	 * @param diagnostics compiler diagnostics for rejected snippets (empty when ok)
	 */
	public record EvalResult( boolean ok, String value, String exception, List<String> diagnostics ) {}

	/**
	 * @return the shared session used by the dev endpoints
	 */
	public static synchronized NGEvalSession shared() {

		if( _shared == null ) {
			_shared = new NGEvalSession();
		}

		return _shared;
	}

	/**
	 * Evaluates the given input, which may contain multiple statements/definitions/expressions.
	 * Evaluation is fail-fast: on the first rejected snippet or thrown exception, the remainder
	 * of the input is not evaluated (later statements usually depend on earlier ones, and a
	 * partial-success report is harder for a tool to act on than a clean stop).
	 */
	public synchronized EvalResult eval( final String input ) {

		if( input == null || input.isBlank() ) {
			return new EvalResult( false, null, null, List.of( "no input" ) );
		}

		final JShell shell = shell();
		final SourceCodeAnalysis analysis = shell.sourceCodeAnalysis();

		String value = null;
		String remaining = input;

		while( remaining != null && !remaining.isBlank() ) {
			final CompletionInfo info = analysis.analyzeCompletion( remaining );

			if( !info.completeness().isComplete() ) {
				return new EvalResult( false, value, null, List.of( "incomplete input: " + remaining.strip() ) );
			}

			final List<SnippetEvent> events = shell.eval( info.source() );

			for( final SnippetEvent event : events ) {
				// Only report on the snippets this eval created — updates to earlier snippets
				// (dependency re-resolution) also arrive as events, but they aren't this input's story.
				if( event.causeSnippet() != null ) {
					continue;
				}

				if( event.status() == Snippet.Status.REJECTED ) {
					final List<String> diagnostics = new ArrayList<>();
					shell.diagnostics( event.snippet() )
							.forEach( diagnostic -> diagnostics.add( diagnostic.getMessage( null ) ) );
					if( diagnostics.isEmpty() ) {
						diagnostics.add( "rejected: " + event.snippet().source().strip() );
					}
					return new EvalResult( false, value, null, diagnostics );
				}

				if( event.exception() != null ) {
					return new EvalResult( false, value, describeException( event.exception() ), List.of() );
				}

				if( event.value() != null ) {
					value = event.value();
				}
			}

			remaining = info.remaining();
		}

		return new EvalResult( true, value, null, List.of() );
	}

	/**
	 * Discards the session — all variables, definitions and imports — and starts fresh on the
	 * next evaluation.
	 */
	public synchronized void reset() {

		if( _shell != null ) {
			_shell.close();
			_shell = null;
		}
	}

	private JShell shell() {

		if( _shell == null ) {
			_shell = JShell.builder()
					.executionEngine( new LocalExecutionControlProvider(), null )
					.build();

			// Seed the compilation classpath from the running JVM's launch classpath, so
			// snippets compile against the application's own classes. Execution then loads
			// them through parent delegation — the same classes the app is running.
			final String classpath = System.getProperty( "java.class.path" );

			if( classpath != null ) {
				for( final String entry : classpath.split( File.pathSeparator ) ) {
					if( !entry.isBlank() ) {
						_shell.addToClasspath( entry );
					}
				}
			}

			// A few default imports so quick exploratory snippets don't start with boilerplate.
			_shell.eval( "import java.util.*;" );
			_shell.eval( "import java.util.stream.*;" );
			_shell.eval( "import java.time.*;" );
		}

		return _shell;
	}

	private static String describeException( final Throwable exception ) {

		if( exception instanceof EvalException evalException ) {
			final String message = evalException.getMessage();
			return evalException.getExceptionClassName() + (message != null ? ": " + message : "");
		}

		if( exception instanceof UnresolvedReferenceException unresolved ) {
			return "unresolved reference in: " + unresolved.getSnippet().name();
		}

		final String message = exception.getMessage();
		return exception.getClass().getName() + (message != null ? ": " + message : "");
	}
}
