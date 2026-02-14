package ng.appserver.templating.elements;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGElement;

/**
 * Represents a plain, non-dynamic HTML string
 */

public class NGHTMLBareString implements NGElement {

	/**
	 * An empty bare string that renders nothing. Useful as a placeholder for stripped content (e.g. template comments).
	 */
	public static final NGHTMLBareString EMPTY = new NGHTMLBareString( "" );

	private final String _string;

	public NGHTMLBareString( final String string ) {
		_string = string;
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		response.appendContentString( _string );
	}
}