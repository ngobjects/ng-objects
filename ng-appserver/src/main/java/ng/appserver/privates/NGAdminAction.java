package ng.appserver.privates;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.directactions.NGDirectAction;

/**
 * Some administrative actions for use in development
 */

public class NGAdminAction extends NGDirectAction {

	public NGAdminAction( NGRequest request ) {
		super( request );
	}

	/**
	 * Overridden to control access
	 */
	@Override
	public NGActionResults performActionNamed( String directActionName ) {
		if( NGApplication.application().isDevelopmentMode() ) {
			return super.performActionNamed( directActionName );
		}

		return new NGResponse( "Admin actions can only be used in development mode", 403 );
	}

	/**
	 * Terminates this application instance and returns a 200 response
	 */
	public NGActionResults terminateAction() {
		NGApplication.application().terminate();
		final NGResponse response = new NGResponse( "terminated", 200 );
		response.setHeader( "content-type", "text/plain" );
		return response;
	}

	/**
	 * @return Just a simple string to indicate that this is an NGObjects application
	 */
	public NGActionResults typeAction() {
		final NGResponse response = new NGResponse( "ng", 200 );
		response.setHeader( "content-type", "text/plain" );
		return response;
	}
}