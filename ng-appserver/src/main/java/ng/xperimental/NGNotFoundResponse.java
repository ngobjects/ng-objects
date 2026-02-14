package ng.xperimental;

import ng.appserver.NGResponse;

/**
 * FIXME: Idea/experimental stage. Marker class returned to signal to the adaptor that the response was not handled // Hugi 2026-02-10
 */

public class NGNotFoundResponse extends NGResponse {

	public NGNotFoundResponse( String contentString, int status ) {
		super( contentString, status );
	}
}