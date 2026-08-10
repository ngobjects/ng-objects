package ng.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestNGDevJson {

	@Test
	public void escapesQuotesAndBackslashes() {
		assertEquals( "say \\\"hi\\\"", NGDevJson.escape( "say \"hi\"" ) );
		assertEquals( "a\\\\b", NGDevJson.escape( "a\\b" ) );
	}

	@Test
	public void escapesWhitespaceControls() {
		assertEquals( "line1\\nline2", NGDevJson.escape( "line1\nline2" ) );
		assertEquals( "a\\tb", NGDevJson.escape( "a\tb" ) );
		assertEquals( "a\\rb", NGDevJson.escape( "a\rb" ) );
	}

	@Test
	public void escapesOtherControlCharacters() {
		assertEquals( "\\u0000", NGDevJson.escape( String.valueOf( (char)0 ) ) );
		assertEquals( "\\u001f", NGDevJson.escape( String.valueOf( (char)0x1f ) ) );
	}

	@Test
	public void nullEscapesToEmpty() {
		assertEquals( "", NGDevJson.escape( null ) );
	}

	@Test
	public void strQuotesOrNullLiteral() {
		assertEquals( "\"hi\"", NGDevJson.str( "hi" ) );
		assertEquals( "null", NGDevJson.str( null ) );
		assertEquals( "\"a\\\"b\"", NGDevJson.str( "a\"b" ) );
	}
}
