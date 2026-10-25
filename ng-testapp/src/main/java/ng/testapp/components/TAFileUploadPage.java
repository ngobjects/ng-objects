package ng.testapp.components;

import java.util.ArrayList;
import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

/**
 * Receiving an uploaded file in full, as a byte[], through NGFileUpload's [data] binding
 */

public class TAFileUploadPage extends NGComponent {

	public record UploadedImage( String filename, byte[] data ) {}

	public List<UploadedImage> images = new ArrayList<>();
	public UploadedImage currentImage;

	/**
	 * Set by the upload field when the form is submitted, before the upload() action is invoked
	 */
	public byte[] uploadedData;
	public String uploadedFilename;

	public TAFileUploadPage( NGContext context ) {
		super( context );
	}

	public NGActionResults upload() {
		images.add( new UploadedImage( uploadedFilename, uploadedData ) );

		uploadedFilename = null;
		uploadedData = null;

		return null;
	}
}