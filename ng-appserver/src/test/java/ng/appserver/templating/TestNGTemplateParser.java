package ng.appserver.templating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.NGAssociation;
import ng.appserver.NGConstantValueAssociation;
import ng.appserver.NGElement;
import ng.appserver.elements.NGString;

public class TestNGTemplateParser {

	@Test
	public void parseSimpleDynamicElement() {
		try {
			Map<String, NGAssociation> associations = Map.of( "value", new NGConstantValueAssociation( "smu" ) );
			NGElement expected = new NGString( null, associations, null );
			NGElement result = new NGTemplateParser( "<wo:str value=\"smu\" />", "" ).parse();
			assertEquals( expected, result );
		}
		catch( NGDeclarationFormatException | NGHTMLFormatException e ) {
			throw new RuntimeException( e );
		}
	}

	/*
	 * FIXME: To be able to test a container element like NGConditional, equivalence of contained template (which comes from NGDynamicGroup) should be checked as well. Or should it? // Hugi 2022-10-07
	 *
		@Test
		public void parseDynamicContainerElement() {
			try {
				NGHTMLBareString containedString = new NGHTMLBareString( "Content textsmu" );
				Map<String, NGAssociation> associations = Map.of( "condition", NGConstantValueAssociation.TRUE );
				NGElement expected = new NGConditional( null, associations, containedString );
				NGElement result = NGTemplateParser.parse( "<wo:if condition=\"$true\">Content text</wo:if>", "", Collections.emptyList() );
				assertEquals( expected, result );
			}
			catch( NGDeclarationFormatException | NGHTMLFormatException e ) {
				throw new RuntimeException( e );
			}
		}
		*/

	@Test
	public void testExtractDeclarationNameWithQuotes() {
		try {
			String declarationName = NGTemplateParser.extractDeclarationName( "<wo name = \"_NGString_4\"" );
			assertEquals( "_NGString_4", declarationName );

			String declarationName2 = NGTemplateParser.extractDeclarationName( "<wo name=\"bleble\"" );
			assertEquals( "bleble", declarationName2 );
		}
		catch( NGHTMLFormatException e ) {
			fail();
		}
	}

	@Test
	public void testExtractDeclarationNameWithoutQuotes() {
		try {
			String declarationName = NGTemplateParser.extractDeclarationName( "<wo name=_NGString_4" );
			assertEquals( "_NGString_4", declarationName );

			String declarationName2 = NGTemplateParser.extractDeclarationName( "<wo name=bleble" );
			assertEquals( "bleble", declarationName2 );
		}
		catch( NGHTMLFormatException e ) {
			fail();
		}
	}
}