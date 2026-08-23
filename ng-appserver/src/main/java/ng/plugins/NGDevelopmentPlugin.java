package ng.plugins;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGRespBuilder;
import ng.appserver.http.NGResponse;
import ng.appserver.privates.NGConsoleCapture;
import ng.dev.NGDevJson;
import ng.dev.NGDevLoopback;
import ng.dev.NGEvalSession;
import ng.dev.NGRuntimeProblems;

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
				.map( "/ng/dev/log", NGDevelopmentPlugin::log )
				.map( "/ng/dev/eval", NGDevelopmentPlugin::eval )
				.map( "/ng/dev/problems", NGDevelopmentPlugin::problems );
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

	/**
	 * Evaluates a Java snippet inside this running application's JVM and returns the result as JSON.
	 * The snippet runs against the application's own live classes and statics (see NGEvalSession), so
	 * a tool can inspect real objects instead of reconstructing them in a separate jshell process.
	 *
	 * The snippet comes from the [snippet] form value (or, for convenience, the raw request body).
	 * [reset=true] discards the persistent session (its variables and definitions) before evaluating.
	 *
	 * Restricted to loopback clients: this is arbitrary code execution in the app's JVM. It's already
	 * dev-mode-only (this whole plugin is), but "dev mode" doesn't imply "only reachable locally", so
	 * we additionally require the caller to be on the loopback interface.
	 */
	private static NGActionResults eval( final NGRequest request ) {

		if( !NGDevLoopback.isLoopback( request.remoteAddress() ) ) {
			return json( "{\"status\":\"error\",\"diagnostics\":[\"/ng/dev/eval is restricted to loopback clients\"]}", 403 );
		}

		if( "true".equals( request.formValueForKey( "reset" ) ) ) {
			NGEvalSession.shared().reset();
		}

		final String snippet = snippetFrom( request );

		// A form-encoded body (curl's default --data) was shredded on '=' and '&' before we saw it,
		// so there's no usable snippet. Don't guess it back together — say what to do instead.
		if( snippet == null && bodyLooksFormEncoded( request ) ) {
			return json( "{\"status\":\"error\",\"diagnostics\":[\"the request body was form-encoded and could not be read as a snippet. Send the snippet as text/plain (curl --data ... -H 'Content-Type: text/plain'), or pass it as the 'snippet' parameter.\"]}", 400 );
		}

		final NGEvalSession.EvalResult result = NGEvalSession.shared().eval( snippet );

		final StringBuilder b = new StringBuilder( 256 );
		b.append( "{\"status\":\"" ).append( result.ok() ? "ok" : "error" ).append( '"' );
		b.append( ",\"value\":" ).append( NGDevJson.str( result.value() ) );
		if( result.exception() != null ) {
			b.append( ",\"exception\":" ).append( NGDevJson.str( result.exception() ) );
		}
		b.append( ",\"diagnostics\":[" );
		for( int i = 0; i < result.diagnostics().size(); i++ ) {
			if( i > 0 ) {
				b.append( ',' );
			}
			b.append( NGDevJson.str( result.diagnostics().get( i ) ) );
		}
		b.append( "]}" );

		return json( b.toString(), 200 );
	}

	/**
	 * Serves the runtime problems the application rendered into its pages (binding-error boxes and
	 * the like) as JSON, so a tool notices them without scraping rendered HTML.
	 *
	 * [contains] filters to problems mentioning the given string; [tail] limits to the last n;
	 * [clear=true] empties the buffer (after snapshotting), useful for marking a clean baseline.
	 */
	private static NGActionResults problems( final NGRequest request ) {

		final String contains = request.formValueForKey( "contains" );
		final int tail = parseInt( request.formValueForKey( "tail" ), 0 );

		final List<NGRuntimeProblems.Problem> problems = NGRuntimeProblems.snapshot( contains, tail );

		if( "true".equals( request.formValueForKey( "clear" ) ) ) {
			NGRuntimeProblems.clear();
		}

		final StringBuilder b = new StringBuilder( problems.size() * 96 + 32 );
		b.append( "{\"problems\":[" );
		for( int i = 0; i < problems.size(); i++ ) {
			final NGRuntimeProblems.Problem problem = problems.get( i );
			if( i > 0 ) {
				b.append( ',' );
			}
			b.append( "{\"time\":" ).append( problem.epochMillis() )
					.append( ",\"kind\":" ).append( NGDevJson.str( problem.kind() ) )
					.append( ",\"element\":" ).append( NGDevJson.str( problem.element() ) )
					.append( ",\"message\":" ).append( NGDevJson.str( problem.message() ) )
					.append( '}' );
		}
		b.append( "],\"count\":" ).append( problems.size() ).append( '}' );

		return json( b.toString(), 200 );
	}

	/**
	 * Extracts the snippet to evaluate: the {@code snippet} form value / query param, or the raw
	 * request body.
	 *
	 * The body MUST be sent as {@code text/plain}. A {@code application/x-www-form-urlencoded} body
	 * (curl's default with {@code --data}) is form-parsed by the adaptor before we see it — split on
	 * {@code =} and {@code &}, which mangles any real Java — so we deliberately do NOT try to
	 * reconstruct a snippet from the shredded form map (reassembly is lossy and a silently-wrong
	 * snippet that runs is worse than a clear error). {@link #bodyLooksFormEncoded} detects that case
	 * so the endpoint can tell the caller exactly what to do instead.
	 *
	 * @return the snippet, or null if none was supplied as a param or a text/plain body
	 */
	private static String snippetFrom( final NGRequest request ) {

		final String param = request.formValueForKey( "snippet" );
		if( param != null && !param.isBlank() ) {
			return param;
		}

		final String body = request.contentString();
		if( body != null && !body.isBlank() ) {
			return body;
		}

		return null;
	}

	/**
	 * @return true when the request carries a form-encoded body (so the raw snippet was consumed and
	 *         shredded into the form map) — a form entry present beyond our own {@code snippet}/{@code reset}
	 *         params, with an empty {@code text/plain}-style body. This is the "you POSTed with the wrong
	 *         content type" signal.
	 */
	private static boolean bodyLooksFormEncoded( final NGRequest request ) {

		final String body = request.contentString();
		if( body != null && !body.isBlank() ) {
			return false; // we got a real body — it wasn't consumed as form data
		}

		for( final String key : request.formValues().keySet() ) {
			if( !"snippet".equals( key ) && !"reset".equals( key ) ) {
				return true;
			}
		}

		return false;
	}

	private static NGResponse json( final String body, final int status ) {
		final NGResponse response = NGRespBuilder.of( body, status );
		response.setHeader( "content-type", "application/json; charset=utf-8" );
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