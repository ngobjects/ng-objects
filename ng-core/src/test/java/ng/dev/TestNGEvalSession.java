package ng.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Each test uses its own session (JShell startup per test is the price of isolation). The
 * same-JVM test is the load-bearing one: it proves an evaluated snippet touches the very same
 * classes/statics as the host application — the property that makes /eval better than an
 * external jshell.
 */
public class TestNGEvalSession {

	private final NGEvalSession _session = new NGEvalSession();

	@AfterEach
	public void closeSession() {
		_session.reset();
	}

	@Test
	public void evaluatesExpression() {
		final NGEvalSession.EvalResult result = _session.eval( "1 + 1" );
		assertTrue( result.ok() );
		assertEquals( "2", result.value() );
		assertNull( result.exception() );
	}

	@Test
	public void stateSurvivesAcrossEvaluations() {
		assertTrue( _session.eval( "int x = 40;" ).ok() );
		final NGEvalSession.EvalResult result = _session.eval( "x + 2" );
		assertTrue( result.ok() );
		assertEquals( "42", result.value() );
	}

	@Test
	public void multipleStatementsInOneInput() {
		final NGEvalSession.EvalResult result = _session.eval( "int a = 1; int b = 2; a + b" );
		assertTrue( result.ok() );
		assertEquals( "3", result.value() );
	}

	@Test
	public void defaultImportsAreAvailable() {
		final NGEvalSession.EvalResult result = _session.eval( "List.of( 1, 2, 3 ).stream().mapToInt( i -> i ).sum()" );
		assertTrue( result.ok() );
		assertEquals( "6", result.value() );
	}

	@Test
	public void compileErrorReportsDiagnostics() {
		final NGEvalSession.EvalResult result = _session.eval( "nosuchvariable" );
		assertFalse( result.ok() );
		assertFalse( result.diagnostics().isEmpty() );
	}

	@Test
	public void runtimeExceptionIsDescribed() {
		final NGEvalSession.EvalResult result = _session.eval( "Integer.parseInt( \"nope\" )" );
		assertFalse( result.ok() );
		assertTrue( result.exception().contains( "NumberFormatException" ) );
	}

	@Test
	public void incompleteInputIsReported() {
		final NGEvalSession.EvalResult result = _session.eval( "if( true ) {" );
		assertFalse( result.ok() );
		assertTrue( result.diagnostics().get( 0 ).startsWith( "incomplete input" ) );
	}

	@Test
	public void resetDiscardsState() {
		assertTrue( _session.eval( "int gone = 1;" ).ok() );
		_session.reset();
		assertFalse( _session.eval( "gone" ).ok() );
	}

	@Test
	public void snippetsRunInTheHostJvmAgainstTheSameClasses() {
		// The snippet mutates a static buffer; the test observes the mutation directly.
		// This only works if the snippet executed in THIS JVM against THIS class — the
		// property the local execution engine exists to provide.
		NGRuntimeProblems.clear();
		final NGEvalSession.EvalResult result = _session.eval( "ng.dev.NGRuntimeProblems.record( \"kind\", \"element\", \"from-eval\" )" );
		assertTrue( result.ok() );
		assertEquals( 1, NGRuntimeProblems.size() );
		assertEquals( "from-eval", NGRuntimeProblems.snapshot( null, 0 ).get( 0 ).message() );
		NGRuntimeProblems.clear();
	}

	@Test
	public void failFastStopsAfterError() {
		// The second statement fails to compile; the third must not run.
		NGRuntimeProblems.clear();
		final NGEvalSession.EvalResult result = _session.eval(
				"int ok = 1; nosuchvariable; ng.dev.NGRuntimeProblems.record( \"k\", \"e\", \"should-not-run\" );" );
		assertFalse( result.ok() );
		assertEquals( 0, NGRuntimeProblems.size() );
	}
}
