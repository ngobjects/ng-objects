package ng.testapp.components;

import java.util.ArrayList;
import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.http.NGMultipartRequest;
import ng.appserver.http.NGMultipartRequest.Part;
import ng.appserver.templating.NGComponent;

/**
 * Receiving several files uploaded with one field (an input with the [multiple] attribute), by reading them from the request.
 *
 * NGFileUpload handles one file per field, so the field is a plain input with a name of our choosing, and the action asks the request for the files uploaded with it.
 */

public class TAFileUploadMultiplePage extends NGComponent {

	public record ReceivedFile( String filename, String contentType, long length ) {}

	/**
	 * The name of the upload field, as used in the template
	 */
	private static final String FIELD_NAME = "files";

	public List<ReceivedFile> receivedFiles = new ArrayList<>();
	public ReceivedFile currentFile;

	public TAFileUploadMultiplePage( NGContext context ) {
		super( context );
	}

	public NGActionResults receive() {

		// A form with enctype="multipart/form-data" is submitted as a multipart request
		if( context().request() instanceof NGMultipartRequest request ) {
			for( final Part file : request.uploadedFilesForKey( FIELD_NAME ) ) {

				// A field submitted without any file chosen sends one empty file with an empty name
				if( !file.filename().isEmpty() ) {
					receivedFiles.add( new ReceivedFile( file.filename(), file.contentType(), file.length() ) );
				}
			}
		}

		return null;
	}
}
