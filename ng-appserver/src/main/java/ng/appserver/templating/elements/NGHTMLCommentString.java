package ng.appserver.templating.elements;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGElement;

/**
 * Represents a comment in rendered output, wrapped in <!-- ... -->
 *
 * Can wrap either a static string (for literal comments) or a dynamic child template (for HTML comments with parsed content).
 */

public class NGHTMLCommentString implements NGElement {

	private final String _staticContent;
	private final NGElement _childTemplate;

	/**
	 * Creates a comment element with static string content (for literal comments)
	 */
	public NGHTMLCommentString( final String staticContent ) {
		_staticContent = staticContent;
		_childTemplate = null;
	}

	/**
	 * Creates a comment element with dynamic child content (for HTML comments containing template content)
	 */
	public NGHTMLCommentString( final NGElement childTemplate ) {
		_staticContent = null;
		_childTemplate = childTemplate;
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		response.appendContentString( "<!--" );

		if( _childTemplate != null ) {
			_childTemplate.appendToResponse( response, context );
		}
		else if( _staticContent != null ) {
			response.appendContentString( _staticContent );
		}

		response.appendContentString( "-->" );
	}
}
