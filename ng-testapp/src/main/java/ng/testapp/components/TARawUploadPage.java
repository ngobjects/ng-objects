package ng.testapp.components;

import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

/**
 * Demonstrates receiving an upload whose body is the file itself, not a form, as apps and scripts often send it. The page's script sends the file to DirectAction.rawUploadAction(), which reads it as a stream.
 */

public class TARawUploadPage extends NGComponent {

	public TARawUploadPage( NGContext context ) {
		super( context );
	}
}
