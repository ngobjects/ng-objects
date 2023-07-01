package ng.appserver.templating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestNGDeclarationParser {

	@Test
	public void _removeOldStyleCommentsFromString1() {
		var testString = "/* Hello */Hugi";
		var result = NGDeclarationParser._removeOldStyleCommentsFromString( testString );
		assertEquals( "Hugi", result );
	}

	@Test
	public void _removeOldStyleCommentsFromString2() {
		var testString = "/* Hello\n */Hugi";
		var result = NGDeclarationParser._removeOldStyleCommentsFromString( testString );
		assertEquals( "Hugi", result );
	}

	@Test
	public void _removeOldStyleCommentsFromString3() {
		var testString = "/* Hello\n */Hugi/*How*/ are /* you */";
		var result = NGDeclarationParser._removeOldStyleCommentsFromString( testString );
		assertEquals( "Hugi are ", result );
	}

	@Test
	public void isNumeric() {
		assertFalse( NGDeclarationParser.isNumeric( "" ) );
		assertTrue( NGDeclarationParser.isNumeric( "-1.345" ) );
		assertTrue( NGDeclarationParser.isNumeric( "+1456" ) );
		assertTrue( NGDeclarationParser.isNumeric( "+1.456987" ) );
		assertTrue( NGDeclarationParser.isNumeric( "-1456987" ) );
		assertTrue( NGDeclarationParser.isNumeric( "1456987" ) );
		assertTrue( NGDeclarationParser.isNumeric( "14.56987" ) );
		assertTrue( NGDeclarationParser.isNumeric( "-.876" ) );
		assertTrue( NGDeclarationParser.isNumeric( "-0.876" ) );
		assertTrue( NGDeclarationParser.isNumeric( ".876" ) );
	}
}