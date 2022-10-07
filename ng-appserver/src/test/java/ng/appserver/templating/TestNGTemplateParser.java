package ng.appserver.templating;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ng.appserver.NGAssociation;
import ng.appserver.NGConstantValueAssociation;
import ng.appserver.NGElement;
import ng.appserver.elements.NGString;

public class TestNGTemplateParser {

	@Test
	public void parse() {
		try {
			Map<String, NGAssociation> associations = Map.of( "value", new NGConstantValueAssociation( "smu" ) );
			NGElement expected = new NGString( null, associations, null );
			NGElement result = NGTemplateParser.parse( "<wo:str value=\"smu\" />", "", Collections.emptyList() );
			assertEquals( expected, result );
		}
		catch( ClassNotFoundException | NGDeclarationFormatException | NGHTMLFormatException e ) {
			throw new RuntimeException( e );
		}
	}
}