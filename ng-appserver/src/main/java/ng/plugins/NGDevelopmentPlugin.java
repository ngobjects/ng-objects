package ng.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGRequest;
import ng.appserver.NGRespBuilder;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGConsoleCapture;

/**
 * Stuff related to development work
 */

public class NGDevelopmentPlugin implements NGPlugin {

	private static final Logger logger = LoggerFactory.getLogger( NGDevelopmentPlugin.class );

	@Override
	public void load( NGApplication application ) {
		// Mirror console output into a ring buffer so /ng/dev/log can serve it back.
		// This plugin only gets loaded in development mode, so no further gating needed.
		NGConsoleCapture.install();
	}

	@Override
	public Routes routes() {
		return Routes
				.create()
				.map( "/ng/dev/type", NGDevelopmentPlugin::type )
				.map( "/ng/dev/terminate", NGDevelopmentPlugin::terminate )
				.map( "/ng/dev/log", NGDevelopmentPlugin::log );
	}

	/**
	 * Serves the application's recently captured console output as plain text, so an external
	 * tool (a script, or an AI agent working in the repo) can read the app's logs over HTTP
	 * instead of a human copying the IDE console by hand.
	 *
	 * Accepts two optional query parameters: [contains] returns only lines containing the given
	 * string (case sensitive), [tail] limits the result to the last n matching lines.
	 */
	private static NGActionResults log( final NGRequest request ) {
		final String contains = request.formValueForKey( "contains" );
		final int tail = parseInt( request.formValueForKey( "tail" ), 0 );

		final var lines = NGConsoleCapture.snapshot( contains, tail );

		final StringBuilder b = new StringBuilder( lines.size() * 80 + 64 );

		for( final String line : lines ) {
			b.append( line ).append( '\n' );
		}

		// A small trailer so a reader knows it reached the end and how much it got
		b.append( "--- " ).append( lines.size() ).append( " line(s)" );

		if( contains != null && !contains.isEmpty() ) {
			b.append( " matching \"" ).append( contains ).append( '"' );
		}

		b.append( " ---\n" );

		final NGResponse response = NGRespBuilder.of( b.toString(), 200 );
		response.setHeader( "content-type", "text/plain; charset=utf-8" );
		return response;
	}

	private static int parseInt( final String value, final int fallback ) {
		if( value == null || value.isEmpty() ) {
			return fallback;
		}

		try {
			return Integer.parseInt( value.trim() );
		}
		catch( final NumberFormatException e ) {
			return fallback;
		}
	}

	/**
	 * Terminates this application instance and returns a 200 response
	 */
	private static NGActionResults terminate() {
		logger.info( "Received a dev application termination request. Goodbye." );
		NGApplication.application().terminate();
		final NGResponse response = NGRespBuilder.of( "terminated", 200 );
		response.setHeader( "content-type", "text/plain" );
		return response;
	}

	/**
	 * @return Just a simple string to indicate that this is an NGObjects application
	 */
	private static NGActionResults type() {
		final NGResponse response = NGRespBuilder.of( "ng", 200 );
		response.setHeader( "content-type", "text/plain" );
		return response;
	}
}