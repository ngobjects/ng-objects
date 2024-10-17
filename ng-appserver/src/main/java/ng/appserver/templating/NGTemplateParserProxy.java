package ng.appserver.templating;

import java.util.Objects;

import ng.appserver.NGElement;

/**
 * Serves as a bridge between the "new and old world" for template parsing 
 */

public class NGTemplateParserProxy {

	private final String _htmlString;
	private final String _wodString;

	public NGTemplateParserProxy( String htmlString, String wodString ) {
		Objects.requireNonNull( htmlString );
		Objects.requireNonNull( wodString );

		_htmlString = htmlString;
		_wodString = wodString;
	}
	
	public NGElement parse() throws NGDeclarationFormatException, NGHTMLFormatException {
		return new NGTemplateParser( _htmlString, _wodString ).parse();
	}
}