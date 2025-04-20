package ng.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGResponse;

/**
 * Stuff related to development work
 */

public class NGDevelopmentPlugin implements NGPlugin {

	private static final Logger logger = LoggerFactory.getLogger( NGDevelopmentPlugin.class );

	@Override
	public Routes routes() {
		return Routes
				.create()
				.map( "/ng/dev/type", NGDevelopmentPlugin::type )
				.map( "/ng/dev/terminate", NGDevelopmentPlugin::terminate );
	}

	/**
	 * Terminates this application instance and returns a 200 response
	 */
	private static NGActionResults terminate() {
		logger.info( "Received a dev application termination request. Goodbye." );
		NGApplication.application().terminate();
		final NGResponse response = new NGResponse( "terminated", 200 );
		response.setHeader( "content-type", "text/plain" );
		return response;
	}

	/**
	 * @return Just a simple string to indicate that this is an NGObjects application
	 */
	private static NGActionResults type() {
		final NGResponse response = new NGResponse( "ng", 200 );
		response.setHeader( "content-type", "text/plain" );
		return response;
	}
}