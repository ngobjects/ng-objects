package ng.appserver.wointegration;

import java.util.regex.Pattern;

import ng.appserver.NGApplication;
import ng.plugins.NGPlugin;
import ng.plugins.Routes;

/**
 * Sets up an NGApplication to speak the language of a WO deployment environment. That entails:
 *
 * - Starting a lifebeat thread to let wotaskd know we're up and about
 * - Adding a URL rewrite pattern to allow us to handle URLs in WO's "adaptor URL" style
 * - Adding a request handler that allows JavaMonitor to stop the application, fetch statistics etc.
 */

public class NGWOIntegrationPlugin implements NGPlugin {

	@Override
	public void load( NGApplication application ) {

		if( application.properties().d().propWOLifebeatEnabled() ) {
			NGDefaultLifeBeatThread.start( application.properties() );

			// What we're doing here is allowing for the WO URL structure, which is required for us to work with the WO Apache Adaptor.
			// Ideally, we don't want to prefix URLs at all, instead just handling requests at root level.

			// CHECKME: Deciding whether to perform URL rewrites based on the lifebeat property is a little iffy // Hugi 2025-05-13
			application.addURLRewritePattern( Pattern.compile( "^/(cgi-bin|Apps)/WebObjects/" + application.properties().d().propWOApplicationName() + ".woa(/[0-9])?" ) );
		}
	}

	@Override
	public Routes routes() {
		return Routes
				.create()
				.map( WOMPRequestHandler.DEFAULT_PATH, new WOMPRequestHandler() );
	}
}