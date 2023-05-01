package ng.kvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class TestNGKeyValueCoding {

	@Test
	public void testValueForKeyOnCollectionsEmptyList() {
		assertEquals( 0, NGKeyValueCoding.Utility.valueForKey( new ArrayList<>(), "size" ) );

		assertEquals( 0, NGKeyValueCoding.Utility.valueForKey( Collections.emptyList(), "size" ) );
		assertEquals( 1, NGKeyValueCoding.Utility.valueForKey( List.of( "Hello" ), "size" ) );
		assertTrue( (boolean)NGKeyValueCoding.Utility.valueForKey( List.of(), "isEmpty" ) );
	}

	@Test
	public void testValueForKeyMethodWithExactName() {
		RecordThatImplementsValueForKey person = new RecordThatImplementsValueForKey( "Hugi" );

		assertEquals( "interfaceValue", NGKeyValueCoding.Utility.valueForKey( person, "getClass" ) );
		assertEquals( "Hugi", NGKeyValueCoding.DefaultImplementation.valueForKey( person, "name" ) );
		assertEquals( RecordThatImplementsValueForKey.class, NGKeyValueCoding.DefaultImplementation.valueForKey( person, "getClass" ) );
		assertNull( NGKeyValueCoding.DefaultImplementation.valueForKey( person, "returnsNull" ) );
	}

	@Test
	public void testValueForKeyMethodWithGetPrefix() {
		PlainOldRecord person = new PlainOldRecord( "Hugi" );
		assertEquals( person.name(), NGKeyValueCoding.Utility.valueForKey( person, "prefixedAccessor" ) );
		assertEquals( person.getClass(), NGKeyValueCoding.Utility.valueForKey( person, "class" ) );
	}

	@Test
	public void testValueForKeyMethodWithIsPrefix() {
		PlainOldRecord person = new PlainOldRecord( "Hugi" );
		assertTrue( (boolean)NGKeyValueCoding.Utility.valueForKey( person, "prefixedBoolean" ) );
		assertEquals( "Hugi", NGKeyValueCoding.Utility.valueForKey( person, "prefixedString" ) );
		assertFalse( (boolean)NGKeyValueCoding.Utility.valueForKey( person.getClass(), "synthetic" ) );
	}

	@Test
	public void testThrowsOnUnknownKey() {
		assertThrows( NGKeyValueCoding.UnknownKeyException.class, () -> {
			NGKeyValueCoding.Utility.valueForKey( new PlainOldRecord( "Hehe" ), "hehe" );
		} );
	}

	@Test
	public void testThrowsExceptionFromTarget() {
		assertThrows( NoSuchElementException.class, () -> {
			NGKeyValueCoding.Utility.valueForKey( List.of().iterator(), "next" );
		} );
	}

	@Test
	public void testValueForKeyFieldWithExactName() {
		Home home = new Home();
		home.address1 = "Hraunteigur 23";

		assertEquals( "Hraunteigur 23", NGKeyValueCoding.DefaultImplementation.valueForKey( home, "address1" ) );
		assertNull( NGKeyValueCoding.DefaultImplementation.valueForKey( home, "address2" ) );
	}

	@Test
	public void testTakeValueForKeyFieldWithExactName() {
		var home = new Home();
		NGKeyValueCoding.Utility.takeValueForKey( home, "Hraunteigur 23", "address1" );
		assertEquals( "Hraunteigur 23", NGKeyValueCoding.Utility.valueForKey( home, "address1" ) );

	}

	@Test
	public void testTakeValueForKeyMethodWithSetPrefixOnly() {
		var home = new Home();
		NGKeyValueCoding.Utility.takeValueForKey( home, "Hraunteigur 23", "address2Method" );
		assertEquals( "Hraunteigur 23", NGKeyValueCoding.Utility.valueForKey( home, "address2" ) );
	}

	@Test
	public void testAssignBigDecimalFieldFromLong() {
		class Product {
			public BigDecimal price;
		}

		Product testProduct = new Product();

		NGKeyValueCoding.Utility.takeValueForKey( testProduct, 50l, "price" );
		assertTrue( testProduct.price.compareTo( new BigDecimal( "50" ) ) == 0 );
	}

	public record RecordThatImplementsValueForKey( String name ) implements NGKeyValueCoding {

		@Override
		public Object valueForKey( String key ) {
			return "interfaceValue";
		}

		public Object returnsNull() {
			return null;
		}

		@Override
		public void takeValueForKey( Object value, String key ) {
			// FIXME: Implement
		}
	}

	public record PlainOldRecord( String name ) {

		public String getPrefixedAccessor() {
			return name();
		}

		public boolean isPrefixedBoolean() {
			return true;
		}

		public String isPrefixedString() {
			return name();
		}
	}

	public static class Home {
		public String address1;
		public String address2;

		public void setAddress2Method( String value ) {
			address2 = value;
		}
	}
}