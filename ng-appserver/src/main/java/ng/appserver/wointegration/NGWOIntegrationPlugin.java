package ng.appserver.wointegration;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGResponse;
import ng.appserver.http.NGResponses;
import ng.appserver.resources.NGResource;
import ng.appserver.resources.StandardNamespace;
import ng.plugins.NGPlugin;
import ng.plugins.Routes;

/**
 * Sets up an NGApplication to speak the language of a WO deployment environment. That entails:
 *
 * - Starting a lifebeat thread to let wotaskd know we're up and about
 * - Adding a URL rewrite pattern to allow us to handle URLs in WO's "adaptor URL" style
 * - Answering wotaskd/JavaMonitor's instance requests, to stop the application, fetch statistics etc.
 */

public class NGWOIntegrationPlugin implements NGPlugin {

	private static final Logger logger = LoggerFactory.getLogger( NGWOIntegrationPlugin.class );

	@Override
	public void load( NGApplication application ) {

		if( application.properties().d().propWOLifebeatEnabled() ) {
			NGLifebeatThreadRunner.start( application.properties() );
		}

		// What we're doing here is allowing for the WO URL structure, which is required for us to work with the WO Apache Adaptor.
		// Ideally, we don't want to prefix URLs at all, instead just handling requests at root level.
		// CHECKME: URL rewrites shouldn't really be performed in development. Add by property? // Hugi 2025-05-13
		application.addURLRewritePattern( Pattern.compile( "^/(cgi-bin|Apps)/WebObjects/(.*).woa(/[0-9])?" ) );
	}

	@Override
	public Routes routes() {
		return Routes
				.create()
				.map( "/womp/instanceRequest", NGWOIntegrationPlugin::instanceRequest );
	}

	/**
	 * Handles an instance request from wotaskd/JavaMonitor
	 *
	 * FIXME: Currently just checking for substrings in the requests's XML content. We should be properly deserializing.
	 */
	private static NGResponse instanceRequest( NGRequest request ) {

		if( request.contentString().contains( "TERMINATE" ) ) {
			return terminate();
		}

		if( request.contentString().contains( "STATISTICS" ) ) {
			return statistics();
		}

		if( request.contentString().contains( "REFUSE" ) ) {
			throw new IllegalArgumentException( "REFUSE operation is currently not supported" );
		}

		throw new IllegalArgumentException( "Unknown admin request: " + request + " content: " + request.contentString() );
	}

	/**
	 * FIXME: We're just returning a hardcoded response at the moment. We'll have to consider if we even want to collect these statistics. A job for JFR perhaps.
	 */
	private static NGResponse statistics() {
		logger.info( "Returning a statistics response. Those are weird..." );

		final Optional<NGResource> resource = NGApplication.application().resourceManager().obtainAppResource( StandardNamespace.NG.identifier(), "x-statistics-response.xml" );
		final byte[] b = resource.get().bytes();
		return NGResponses.ok( b );
	}

	/**
	 * Terminates the application by request from wotaskd and returns a success response
	 */
	private static NGResponse terminate() {
		logger.info( "Terminating application by request from wotaskd..." );

		logger.info( "Sending willStop..." );
		NGLifebeatThreadRunner.lifebeatThread().sendWillStop();
		logger.info( "Sent willstop." );

		// We perform the shutdown in a thread that executes after we've returned the response to the client.
		// This ensures the application has the opportunity to submit a response to wotaskd before shutting down.
		// Not that pretty but does the job.
		// FIXME: The entire shutdown process should probably be in a shutdown hook // Hugi 2024-08-10
		new Thread( () -> {
			try {
				Thread.sleep( 1000 );
				logger.info( "Exiting" );
				System.exit( 0 );
			}
			catch( final InterruptedException e ) {
				e.printStackTrace();
			}
		} ).start();

		logger.info( "sending command response to wotaskd" );

		final NGResponse response = NGResponses.of();
		response.setContentString( """
				<instanceResponse type="NSDictionary">
					<commandInstanceResponse type="NSDictionary">
						<success type="NSString">YES</success>
					</commandInstanceResponse>
				</instanceResponse>""" );
		return response;
	}
}
