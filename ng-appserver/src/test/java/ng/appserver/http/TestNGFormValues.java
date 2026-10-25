package ng.appserver.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;


public class TestNGFormValues {

	private static Map<String, List<String>> parse( final String input ) {
		return parse( input, StandardCharsets.UTF_8 );
	}

	private static Map<String, List<String>> parse( final String input, final Charset charset ) {
		return NGFormValues.parse( input.getBytes( StandardCharsets.ISO_8859_1 ), charset, 1000 );
	}

	@Test
	public void simple() {
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "2" ) ), parse( "a=1&b=2" ) );
	}

	@Test
	public void empty() {
		assertEquals( Map.of(), parse( "" ) );
		assertEquals( Map.of(), parse( "&" ) );
		assertEquals( Map.of(), parse( "&&&" ) );
	}

	@Test
	public void emptyFieldsAreSkipped() {
		assertEquals( Map.of( "a", List.of( "1" ), "b", List.of( "2" ) ), parse( "&a=1&&b=2&" ) );
	}

	@Test
	public void repeatedNamesKeepEveryValueInOrder() {
		assertEquals( Map.of( "a", List.of( "1", "3" ), "b", List.of( "2" ) ), parse( "a=1&b=2&a=3" ) );
	}

	@Test
	public void namesKeepTheirOrder() {
		assertEquals( List.of( "z", "a", "m" ), List.copyOf( parse( "z=1&a=2&m=3" ).keySet() ) );
	}

	@Test
	public void fieldWithoutEqualsHasEmptyValue() {
		assertEquals( Map.of( "a", List.of( "" ) ), parse( "a" ) );
		assertEquals( Map.of( "a", List.of( "" ) ), parse( "a=" ) );
	}

	@Test
	public void emptyName() {
		assertEquals( Map.of( "", List.of( "1" ) ), parse( "=1" ) );
		assertEquals( Map.of( "", List.of( "" ) ), parse( "=" ) );
	}

	@Test
	public void onlyTheFirstEqualsSplits() {
		assertEquals( Map.of( "a", List.of( "1=2==3" ) ), parse( "a=1=2==3" ) );
	}

	@Test
	public void plusIsSpace() {
		assertEquals( Map.of( "a b", List.of( "c d" ) ), parse( "a+b=c+d" ) );
	}

	@Test
	public void escapedPlusIsPlus() {
		assertEquals( Map.of( "a", List.of( "1+1" ) ), parse( "a=1%2B1" ) );
	}

	@Test
	public void escapedSeparatorsAreData() {
		assertEquals( Map.of( "a&b", List.of( "c=d" ) ), parse( "a%26b=c%3Dd" ) );
	}

	@Test
	public void escapesAreCaseInsensitive() {
		assertEquals( Map.of( "a", List.of( "//" ) ), parse( "a=%2f%2F" ) );
	}

	@Test
	public void utf8() {
		assertEquals( Map.of( "nafn", List.of( "Þórður" ) ), parse( "nafn=%C3%9E%C3%B3r%C3%B0ur" ) );
	}

	@Test
	public void rawUTF8Bytes() {
		final byte[] input = "nafn=Þórður".getBytes( StandardCharsets.UTF_8 );
		assertEquals( Map.of( "nafn", List.of( "Þórður" ) ), NGFormValues.parse( input, StandardCharsets.UTF_8, 1000 ) );
	}

	@Test
	public void malformedEscapesAreKept() {
		assertEquals( Map.of( "a", List.of( "%" ) ), parse( "a=%" ) );
		assertEquals( Map.of( "a", List.of( "%2" ) ), parse( "a=%2" ) );
		assertEquals( Map.of( "a", List.of( "%zz" ) ), parse( "a=%zz" ) );
		assertEquals( Map.of( "a", List.of( "100%" ) ), parse( "a=100%" ) );
		assertEquals( Map.of( "a", List.of( "%A" ) ), parse( "a=%%41" ) );
	}

	@Test
	public void escapeIsNotReadAcrossFieldBoundaries() {
		assertEquals( Map.of( "a", List.of( "%4" ), "1", List.of( "" ) ), parse( "a=%4&1" ) );
	}

	@Test
	public void invalidBytesBecomeReplacementCharacters() {
		assertEquals( Map.of( "a", List.of( "�" ) ), parse( "a=%FF" ) );
	}

	@Test
	public void otherCharset() {
		assertEquals( Map.of( "nafn", List.of( "Þórður" ) ), parse( "nafn=%DE%F3r%F0ur", StandardCharsets.ISO_8859_1 ) );
	}

	@Test
	public void fieldLimit() {
		assertEquals( 3, NGFormValues.parse( "a=1&b=2&c=3".getBytes( StandardCharsets.UTF_8 ), StandardCharsets.UTF_8, 3 ).size() );
		assertThrows( IllegalArgumentException.class, () -> NGFormValues.parse( "a=1&b=2&c=3&d=4".getBytes( StandardCharsets.UTF_8 ), StandardCharsets.UTF_8, 3 ) );
	}

	@Test
	public void repeatedNamesCountTowardsTheFieldLimit() {
		assertThrows( IllegalArgumentException.class, () -> NGFormValues.parse( "a=1&a=2&a=3&a=4".getBytes( StandardCharsets.UTF_8 ), StandardCharsets.UTF_8, 3 ) );
	}

	@Test
	public void emptyFieldsDontCountTowardsTheFieldLimit() {
		assertEquals( 1, NGFormValues.parse( "&&&&a=1&&&&".getBytes( StandardCharsets.UTF_8 ), StandardCharsets.UTF_8, 1 ).size() );
	}
}
