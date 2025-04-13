package ng.appserver;

import java.util.HashMap;
import java.util.Map;

/**
 * FIXME: Created as a temporary holder class to experiment with a nice API for constructing multipart responses
 */

public class NGResponseMultipart extends NGResponse {

	/**
	 * We need the ontext to know which part of the page we're targeting if the request targetd multiple update containers
	 */
	public NGContext context;

	/**
	 * The list of
	 */
	public Map<String, ContentPart> _contentParts = new HashMap<>();

	public record ContentPart( String name, String contentType, StringBuilder content ) {}

	private String updateContainerToTarget() {
		if( context.targetsMultipleUpdateContainers() ) {
			// The list of containers to update is passed in to the request as a header
			final String containerIDToUpdate = context.targetedUpdateContainerID();

			final String[] updateContainerIDs = containerIDToUpdate.split( ";" );

			// We return the first matching container, since th should be the outermost container
			for( String containingContainerID : context.containingUpdateContainerIDs ) {
				for( String targetedContainerID : updateContainerIDs ) {
					if( containingContainerID.equals( targetedContainerID ) ) {
						return containingContainerID;
					}
				}
			}
			for( String updateContainerID : updateContainerIDs ) {
				if( context.containingUpdateContainerIDs.contains( updateContainerID ) ) {
					return updateContainerID;
				}
			}

		}

		return null;
	}

	@Override
	public void appendContentString( String stringToAppend ) {
		if( context.targetsMultipleUpdateContainers() ) {
			contentPart( updateContainerToTarget() ).content().append( stringToAppend );
		}
		else {
			super.appendContentString( stringToAppend );
		}
	}

	/**
	 * @return The content part with the given name. If no such part exists, construct a new one
	 */
	private ContentPart contentPart( String name ) {
		ContentPart contentPart = _contentParts.get( name );

		if( contentPart == null ) {
			contentPart = new ContentPart( name, "text/html", new StringBuilder() );
			_contentParts.put( name, contentPart );
		}

		return contentPart;
	}
}
