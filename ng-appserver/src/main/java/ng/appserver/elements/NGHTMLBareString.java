package ng.appserver.elements;

import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

/**
 * Represents a plain, non-dynamic HTML string
 */

public class NGHTMLBareString extends NGElement {

	private final String _string;

	public NGHTMLBareString( final String string ) {
		_string = string;
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		response.appendContentString( _string );
	}
}