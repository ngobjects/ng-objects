package ng.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The buffer is global (deliberately — one store per JVM), so each test starts from a clean slate.
 */
public class TestNGRuntimeProblems {

	@BeforeEach
	public void clearBuffer() {
		NGRuntimeProblems.clear();
	}

	@Test
	public void recordAndSnapshotOldestFirst() {
		NGRuntimeProblems.record( "Unknown key", "WOString", "first" );
		NGRuntimeProblems.record( "Unknown key", "WOString", "second" );

		final List<NGRuntimeProblems.Problem> problems = NGRuntimeProblems.snapshot( null, 0 );
		assertEquals( 2, problems.size() );
		assertEquals( "first", problems.get( 0 ).message() );
		assertEquals( "second", problems.get( 1 ).message() );
		assertTrue( problems.get( 0 ).epochMillis() > 0 );
	}

	@Test
	public void containsFiltersAcrossKindElementAndMessage() {
		NGRuntimeProblems.record( "Unknown key", "WOString", "no such key 'name'" );
		NGRuntimeProblems.record( "Binding configuration error", "WORepetition", "list required" );

		assertEquals( 1, NGRuntimeProblems.snapshot( "Repetition", 0 ).size() );
		assertEquals( 1, NGRuntimeProblems.snapshot( "no such key", 0 ).size() );
		assertEquals( 1, NGRuntimeProblems.snapshot( "configuration", 0 ).size() );
		assertEquals( 0, NGRuntimeProblems.snapshot( "nowhere", 0 ).size() );
	}

	@Test
	public void tailReturnsMostRecent() {
		for( int i = 0; i < 5; i++ ) {
			NGRuntimeProblems.record( "k", "e", "message-" + i );
		}

		final List<NGRuntimeProblems.Problem> problems = NGRuntimeProblems.snapshot( null, 2 );
		assertEquals( 2, problems.size() );
		assertEquals( "message-3", problems.get( 0 ).message() );
		assertEquals( "message-4", problems.get( 1 ).message() );
	}

	@Test
	public void bufferIsBounded() {
		for( int i = 0; i < 1100; i++ ) {
			NGRuntimeProblems.record( "k", "e", "message-" + i );
		}

		assertEquals( 1000, NGRuntimeProblems.size() );
		// The oldest entries were dropped; the newest survive.
		final List<NGRuntimeProblems.Problem> problems = NGRuntimeProblems.snapshot( null, 1 );
		assertEquals( "message-1099", problems.get( 0 ).message() );
	}

	@Test
	public void clearEmptiesTheBuffer() {
		NGRuntimeProblems.record( "k", "e", "m" );
		NGRuntimeProblems.clear();
		assertEquals( 0, NGRuntimeProblems.size() );
	}

	@Test
	public void nullFieldsBecomeEmpty() {
		NGRuntimeProblems.record( null, null, null );
		final NGRuntimeProblems.Problem problem = NGRuntimeProblems.snapshot( null, 0 ).get( 0 );
		assertEquals( "", problem.kind() );
		assertEquals( "", problem.element() );
		assertEquals( "", problem.message() );
	}
}
