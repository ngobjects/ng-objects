package ng.kvc;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ng.kvc.NGKeyValueCoding.UnknownKeyException;

public class TestNGKeyValueCodingAdditions {

	@Test
	public void valueForKeyPath() {
		var p = new Person( "Hugi", new Address( "Reykjavík" ) );
		assertEquals( 4, NGKeyValueCodingAdditions.DefaultImplementation.valueForKeyPath( p, "name.toLowerCase.toUpperCase.toLowerCase.length" ) );
		assertEquals( "Reykjavík", NGKeyValueCodingAdditions.DefaultImplementation.valueForKeyPath( p, "address.city" ) );
	}

	@Test
	public void valueForKeyPathThrowsUnknownKeyException() {
		var p = new Person( "Hugi", new Address( "Reykjavík" ) );

		assertThrows( UnknownKeyException.class, () -> {
			NGKeyValueCodingAdditions.DefaultImplementation.valueForKeyPath( p, "bla.smu.fne" );
		} );

		assertThrows( UnknownKeyException.class, () -> {
			NGKeyValueCodingAdditions.DefaultImplementation.valueForKeyPath( p, "address.citi" );
		} );

		assertThrows( UnknownKeyException.class, () -> {
			NGKeyValueCodingAdditions.DefaultImplementation.valueForKeyPath( p, "adres.city" );
		} );
	}

	@Test
	public void takeValueForKeyPath() {
		var p = new Person( "Hugi", new Address( "Reykjavík" ) );
		NGKeyValueCodingAdditions.DefaultImplementation.takeValueForKeyPath( p, "Neskaupstaður", "address.city" );
		assertEquals( "Neskaupstaður", p.address.city );
	}

	@Test
	public void takeNullValueForKeyPath() {
		var p = new Person( "Hugi", new Address( "Reykjavík" ) );
		NGKeyValueCodingAdditions.DefaultImplementation.takeValueForKeyPath( p, null, "address.city" );
		assertNull( p.address.city );
	}

	public static class Person {
		public String name;
		public Address address;

		public Person( String name, Address address ) {
			this.name = name;
			this.address = address;
		}
	}

	public static class Address {
		public String city;

		public Address( String city ) {
			this.city = city;
		}
	}
}