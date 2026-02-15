package ng.appserver.templating.elements;


/**
 * Represents a plain HTML comment in rendered output, wrapped in <!-- ... -->
 */

public class NGHTMLCommentString extends NGHTMLBareString {

	public NGHTMLCommentString( final String string ) {
		super( "<!--" + string + "-->" );
	}
}
