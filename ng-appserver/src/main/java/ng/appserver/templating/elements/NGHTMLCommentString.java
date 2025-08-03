package ng.appserver.templating.elements;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;

/**
 * Represents a comment string.
 */

public class NGHTMLCommentString extends NGHTMLBareString {

	public NGHTMLCommentString( String aString ) {
		super( aString );
	}

	@Override
	public void appendToResponse( NGResponse aResponse, NGContext aContext ) {
		super.appendToResponse( aResponse, aContext );
	}
}
