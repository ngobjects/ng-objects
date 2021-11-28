package ng.kvc.benchmarks;

import ng.kvc.NGKeyValueCoding;

public class KVCBenchmark {

	public static void main( String[] argv ) {
		run();
		run();
		run();
	}

	public static void run() {
		Person person = new Person( "Hugi" );

		int numberOfRuns = 10_000_000;

		long start = System.currentTimeMillis();

		for( int i = 0; i < numberOfRuns; i++ ) {
			Object valueForKey = NGKeyValueCoding.Utility.valueForKey( person, "name" );
			int length = ((String)valueForKey).length();
		}

		System.out.println( "Time: " + (System.currentTimeMillis() - start) );
	}

	public record Person( String name ) {}
}