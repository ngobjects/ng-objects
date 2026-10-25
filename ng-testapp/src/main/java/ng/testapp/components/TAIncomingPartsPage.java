package ng.testapp.components;

import java.util.List;

import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

/**
 * Demonstrates reading a multipart request's parts as they arrive, for uploads too large to collect. The form posts to DirectAction.incomingPartsAction(), which does the reading and returns this page with what it received.
 */

public class TAIncomingPartsPage extends NGComponent {

	/**
	 * What the direct action received from one part
	 *
	 * @param millis How long reading the part took. For a large file, that's roughly how long it took to arrive.
	 */
	public record ReceivedPart( String name, String filename, String contentType, long length, long millis ) {}

	/**
	 * The parts received. null when the page is shown without an upload.
	 */
	public List<ReceivedPart> receivedParts;
	public ReceivedPart currentPart;

	/**
	 * The value of the [folder] query parameter the form posted with
	 */
	public String folder;

	public TAIncomingPartsPage( NGContext context ) {
		super( context );
	}
}
