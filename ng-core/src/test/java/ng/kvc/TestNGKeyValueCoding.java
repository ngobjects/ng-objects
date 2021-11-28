package ng.kvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestNGKeyValueCoding {

	@Test
	public void testMethodAccess() {
		Person person = new Person( "Hugi" );

		assertEquals( "interfaceValue", NGKeyValueCoding.Utility.valueForKey( person, "getClass" ) );
		assertEquals( "Hugi", NGKeyValueCoding.DefaultImplementation.valueForKey( person, "name" ) );
		assertEquals( Person.class, NGKeyValueCoding.DefaultImplementation.valueForKey( person, "getClass" ) );
	}

	public record Person( String name ) implements NGKeyValueCoding {

		@Override
		public Object valueForKey( String key ) {
			return "interfaceValue";
		}

		@Override
		public void takeValueForKey( Object value, String key ) {
			// FIXME: Implement
		}
	}
}