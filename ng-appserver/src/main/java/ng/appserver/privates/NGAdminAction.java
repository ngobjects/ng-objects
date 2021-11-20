package ng.appserver.privates;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGDirectAction;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGAdminAction extends NGDirectAction {

	public NGAdminAction( NGRequest request ) {
		super( request );
	}

	/**
	 * FIXME: We probably want to return a structured response here, a la WOMPRequestHandler
	 * FIXME: We probably want to control access to this. Having a public kill switch isn't perhaps… bright.
	 */
	public NGActionResults terminateAction() {
		NGApplication.application().terminate();
		return new NGResponse( "Application successfully terminated", 200 );
	}
}