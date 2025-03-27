package ng.kvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestNGKeyValueCodingSupport {

	@Test
	public void testNormalizeGetterMethodName() {
		assertEquals( "get", NGKeyValueCodingSupport.keyForMethodName( "get" ) );
		assertEquals( "get", NGKeyValueCodingSupport.keyForMethodName( "_get" ) );

		assertEquals( "is", NGKeyValueCodingSupport.keyForMethodName( "is" ) );
		assertEquals( "is", NGKeyValueCodingSupport.keyForMethodName( "_is" ) );

		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "_name" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "_getName" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "isName" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "_isName" ) );
		assertEquals( "name", NGKeyValueCodingSupport.keyForMethodName( "getName" ) );
	}
}