package ng.kvc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TestNGKeyValueCodingSupport {

	@Test
	public void testKeyForMethodName() {
		assertEquals( "get", NGKeyValueCodingSupport.keyForMethodName( "get" ) );
		assertEquals( "get", NGKeyValueCodingSupport.keyForMethodName( "_get" ) );
		assertEquals( "is", NGKeyValueCodingSupport.keyForMethodName( "is" ) );
		assertEquals( "is", NGKeyValueCodingSupport.keyForMethodName( "_is" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "name" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "_name" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "_getName" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "getName" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "_isName" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "isName" ) );
	}

	@Test
	public void testKeyForFieldName() {
		assertEquals( "get", NGKeyValueCodingSupport.keyForFieldName( "get" ) );
		assertEquals( "get", NGKeyValueCodingSupport.keyForFieldName( "_get" ) );

		assertEquals( "is", NGKeyValueCodingSupport.keyForFieldName( "is" ) );
		assertEquals( "is", NGKeyValueCodingSupport.keyForFieldName( "_is" ) );

		assertEquals( "name", NGKeyValueCodingSupport.keyForFieldName( "_name" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForFieldName( "isName" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForFieldName( "_isName" ) );

		// Just check that our method name conventions aren't leaking through...
		assertEquals( "getName", NGKeyValueCodingSupport.keyForFieldName( "_getName" ) );
		assertEquals( "getName", NGKeyValueCodingSupport.keyForFieldName( "getName" ) );
	}

	@Test
	public void testGetterKeysForObject() {
		final List<String> availableKeys = NGKeyValueCodingSupport.getterKeysForObject( new _TestSomeKVCClass() );

		assertTrue( availableKeys.contains( "publicField" ) );
		assertTrue( availableKeys.contains( "publicMethod" ) );

		assertFalse( availableKeys.contains( "friendlyField" ) );
		assertFalse( availableKeys.contains( "privateField" ) );
		assertFalse( availableKeys.contains( "protectedField" ) );

		assertFalse( availableKeys.contains( "friendlyMethod" ) );
		assertFalse( availableKeys.contains( "privateMethod" ) );
		assertFalse( availableKeys.contains( "protectedMethod" ) );
	}

	public static class _TestSomeKVCClass {

		String friendlyField;
		private String privateField;
		protected String protectedField;
		public String publicField;

		String friendlyMethod() {
			return "";
		}

		protected String privateMethod() {
			return "";
		}

		protected String protectedMethod() {
			return "";
		}

		public String publicMethod() {
			return "";
		}
	}
}