package ng.appserver.wointegration;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.NGRequest;
import ng.appserver.NGRequestHandler;
import ng.appserver.NGResponse;

/**
 * Responds to wotaskd/Monitor requests for:
 *
 *  - Stopping the application
 *  - Gathering statistics
 */

public class WOMPRequestHandler extends NGRequestHandler {

	private static final Logger logger = LoggerFactory.getLogger( WOMPRequestHandler.class );

	public static final String DEFAULT_PATH = "/womp/instanceRequest";

	/**
	 * FIXME: Currently just checking for substrings in the requests's XML content. We should be properly deserializing.
	 */
	@Override
	public NGResponse handleRequest( NGRequest request ) {

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
	private NGResponse statistics() {
		logger.info( "Returning a statistics response. Those are weird..." );

		final Optional<byte[]> bytes = NGApplication.application().resourceManager().bytesForAppResourceNamed( "x-statistics-response.xml" );
		final byte[] b = bytes.get();
		return new NGResponse( b, 200 );
	}

	/**
	 * Terminates the application by request from wotaskd and returns a success response
	 */
	private static NGResponse terminate() {
		logger.info( "Terminating application by request from wotaskd..." );

		logger.info( "Sending willStop..." );
		NGDefaultLifeBeatThread._lifebeatThread.sendMessage( NGDefaultLifeBeatThread._lifebeatThread._messageGenerator._willStop );
		logger.info( "Sent willstop." );

		// We perform the shutdown in a thread that executes after we've returned the response to the client.
		// This ensures the application has the opportunity to submit a response to wotaskd before shutting down.
		// Not that pretty but does the job.
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

		final NGResponse response = new NGResponse();
		response.setContentString( "<instanceResponse type=\"NSDictionary\">\n"
				+ "	<commandInstanceResponse type=\"NSDictionary\">\n"
				+ "		<success type=\"NSString\">YES</success>\n"
				+ "	</commandInstanceResponse>\n"
				+ "</instanceResponse>" );
		return response;
	}
}