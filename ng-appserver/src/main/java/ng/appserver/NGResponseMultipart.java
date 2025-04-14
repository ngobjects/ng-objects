package ng.appserver;

import java.util.HashMap;
import java.util.Map;

/**
 * Holder class constructing responses with a multipart body.
 *
 * FIXME:
 * This class is a bit of a standout, since as the framework is designed at the moment, NGResponse usually contains the data directly.
 * Ideally we'd have a separate container class for the response's body content (string/data/multipart), like most request handling frameworks do.
 * Until a decision is made on the final design of the APIs, this class gets to stay.
 * Hugi 2025-04-14
 */

@Deprecated
public class NGResponseMultipart extends NGResponse {

	/**
	 * We need the context to keep track of which parts of the page are targeted for update
	 */
	private NGContext _context;

	/**
	 * The parts this response contains. Initialized to an empty list at the start. If used in NGResponse, should probably be null if this isn't a multipart response (in our current worldview)
	 */
	public Map<String, ContentPart> _contentParts = new HashMap<>();

	public NGResponseMultipart( NGContext context ) {
		_context = context;
	}

	/**
	 * A single part of content within a multipart response
	 */
	public record ContentPart( String name, StringBuilder content ) {}

	@Override
	public void appendContentString( String stringToAppend ) {
		if( _context.targetsMultipleUpdateContainers() ) {
			getContentPart( _context.updateContainerToAppendTo() ).content().append( stringToAppend );
		}
		else {
			super.appendContentString( stringToAppend );
		}
	}

	/**
	 * @return The content part with the given name. If no such part exists, construct a new one
	 */
	private ContentPart getContentPart( final String name ) {
		ContentPart contentPart = _contentParts.get( name );

		if( contentPart == null ) {
			contentPart = new ContentPart( name, new StringBuilder() );
			_contentParts.put( name, contentPart );
		}

		return contentPart;
	}
}