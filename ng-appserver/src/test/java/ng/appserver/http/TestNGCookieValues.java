package ng.appserver.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;


public class TestNGCookieValues {

	private static Map<String, List<String>> parse( final String... headerValues ) {
		return NGCookieValues.parse( List.of( headerValues ) );
	}

	@Test
	public void simple() {
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "2" ) ), parse( "a=1; b=2" ) );
	}

	@Test
	public void noHeaders() {
		assertEquals( Map.of(), parse() );
		assertEquals( Map.of(), parse( "" ) );
	}

	@Test
	public void whitespaceIsIgnored() {
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "2" ) ), parse( "a=1;b=2" ) );
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "2" ) ), parse( "  a = 1 ;  b=2  " ) );
	}

	@Test
	public void emptyPairsAreSkipped() {
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "2" ) ), parse( ";a=1;;b=2;" ) );
	}

	@Test
	public void severalHeaders() {
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "2" ) ), parse( "a=1", "b=2" ) );
	}

	@Test
	public void repeatedNamesKeepEveryValueInOrder() {
		assertEquals( Map.of( "a", List.of( "1", "2" ) ), parse( "a=1; a=2" ) );
		assertEquals( Map.of( "a", List.of( "1", "2" ) ), parse( "a=1", "a=2" ) );
	}

	@Test
	public void namesAreCaseSensitive() {
		assertEquals( Map.of( "A", List.of( "1" ), "a", List.of( "2" ) ), parse( "A=1; a=2" ) );
	}

	@Test
	public void onlyTheFirstEqualsSplits() {
		assertEquals( Map.of( "a", List.of( "x=y==" ) ), parse( "a=x=y==" ) );
	}

	@Test
	public void emptyValue() {
		assertEquals( Map.of( "a", List.of( "" ) ), parse( "a=" ) );
		assertEquals( Map.of( "a", List.of( "" ) ), parse( "a=\"\"" ) );
	}

	@Test
	public void quotesAreStripped() {
		assertEquals( Map.of( "a", List.of( "quoted" ) ), parse( "a=\"quoted\"" ) );
	}

	@Test
	public void unbalancedQuotesAreKept() {
		assertEquals( Map.of( "a", List.of( "\"open" ), "b", List.of( "2" ) ), parse( "a=\"open; b=2" ) );
		assertEquals( Map.of( "a", List.of( "\"" ) ), parse( "a=\"" ) );
	}

	@Test
	public void valuesArentDecoded() {
		assertEquals( Map.of( "a", List.of( "%41+b" ) ), parse( "a=%41+b" ) );
	}

	@Test
	public void spacesCommasAndOtherCharactersAreKept() {
		assertEquals( Map.of( "a", List.of( "hello world" ) ), parse( "a=hello world" ) );
		assertEquals( Map.of( "a", List.of( "1, b=2" ) ), parse( "a=1, b=2" ) );
		assertEquals( Map.of( "a", List.of( "Þór" ) ), parse( "a=Þór" ) );
	}

	@Test
	public void pairsThatArentCookiesAreSkipped() {
		assertEquals( Map.of( "b", List.of( "2" ) ), parse( "noequals; b=2" ) );
		assertEquals( Map.of( "b", List.of( "2" ) ), parse( " =1; b=2" ) );
	}

	@Test
	public void legacyAttributesAreJustCookies() {
		assertEquals( Map.of( "$Version", List.of( "1" ), "a", List.of( "1" ), "$Path", List.of( "/" ) ), parse( "$Version=1; a=1; $Path=/" ) );
	}

	@Test
	public void namesKeepTheirOrder() {
		assertEquals( List.of( "z", "a", "m" ), List.copyOf( parse( "z=1; a=2; m=3" ).keySet() ) );
	}
}
