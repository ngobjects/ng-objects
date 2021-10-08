package ng.kvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestNGKeyValueCoding {

	@Test
	public void testMethodAccess() {
		Person person = new Person();
		person._name = "Hugi";
		
		assertEquals( "Hugi", NGKeyValueCoding.Utility.valueForKey( person, "name" ) );
	}
	
	public static class Person {
		public String _name;
		
		public String name() {
			return _name;
		}
	}
}