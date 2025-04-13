package ng.appserver;

import java.util.HashMap;
import java.util.Map;

/**
 * FIXME: Created as a temporary holder class to experiment with a nice API for constructing multipart responses
 */

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

	private String updateContainerToTarget() {
		if( _context.targetsMultipleUpdateContainers() ) {
			// The list of containers to update is passed in to the request as a header
			final String containerIDToUpdate = _context.targetedUpdateContainerID();

			final String[] updateContainerIDs = containerIDToUpdate.split( ";" );

			// We return the first matching container, since that should be the outermost container
			for( String containingContainerID : _context.containingUpdateContainerIDs ) {
				for( String targetedContainerID : updateContainerIDs ) {
					if( containingContainerID.equals( targetedContainerID ) ) {
						return containingContainerID;
					}
				}
			}
		}

		return null;
	}

	@Override
	public void appendContentString( String stringToAppend ) {
		if( _context.targetsMultipleUpdateContainers() ) {
			getContentPart( updateContainerToTarget() ).content().append( stringToAppend );
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
