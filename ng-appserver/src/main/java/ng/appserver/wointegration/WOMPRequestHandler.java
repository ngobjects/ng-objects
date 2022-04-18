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

	public static final String KEY = "womp";

	/**
	 * FIXME: Still just checking for substrings here. We should be properly deserializing the request.
	 */
	@Override
	public NGResponse handleRequest( NGRequest request ) {

		if( request.contentString().contains( "TERMINATE" ) ) {
			return terminate();
		}

		if( request.contentString().contains( "STATISTICS" ) ) {
			return statistics();
		}

		logger.info( "Unknown admin request" );

		return new NGResponse();
	}

	private NGResponse statistics() {
		logger.info( "Returning a statistics response. Those are weird..." );

		final Optional<byte[]> bytes = NGApplication.application().resourceManager().bytesForResourceNamed( "x-statistics-response.xml" );
		final byte[] b = bytes.get();
		return new NGResponse( b, 200 );
	}

	private static NGResponse terminate() {
		logger.info( "Terminating application by request from wotaskd..." );
		logger.info( "Sending willStop..." );
		NGDefaultLifeBeatThread._lifebeatThread.sendMessage( NGDefaultLifeBeatThread._lifebeatThread._messageGenerator._willStop );
		logger.info( "Sent willstop." );

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