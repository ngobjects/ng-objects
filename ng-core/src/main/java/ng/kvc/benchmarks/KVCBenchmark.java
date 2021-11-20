package ng.kvc.benchmarks;

import ng.kvc.NGKeyValueCoding;

public class KVCBenchmark {

	public static void main( String[] argv ) {
		Person person = new Person();
		person._name = "Hugi";

		int numberOfRuns = 10_000_000;

		long start = System.currentTimeMillis();
		for( int i = 0; i < numberOfRuns; i++ ) {
			Object valueForKey = NGKeyValueCoding.Utility.valueForKey( person, "name" );
			int inti = ((String)valueForKey).length();
		}

		System.out.println( "Time: " + (System.currentTimeMillis() - start) );
	}

	public static class Person {
		public String _name;

		public String name() {
			return _name;
		}
	}
}