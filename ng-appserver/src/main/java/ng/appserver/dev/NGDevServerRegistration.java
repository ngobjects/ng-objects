package ng.appserver.dev;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;

/**
 * Announces this application's name, port and pid to the IDE dev server (Parslips/WOLips,
 * listening on localhost) at startup, so external tools and AI agents can discover where the
 * app is running — by querying the dev server's /apps endpoint — instead of being told the
 * port or guessing.
 *
 * The dev server's location is read from the "wolips.port" property (the same convention
 * wonder-slim and the IDE-side tooling use), defaulting to 9485.
 *
 * Best-effort by design: the dev server may not be running (no Eclipse, CI, production).
 * Registration must never affect the app — it runs on a short-timeout background thread and
 * swallows every failure. Callers gate it on development mode; there's no reason to announce
 * a production app to a developer's IDE.
 */

public class NGDevServerRegistration {

	private static final Logger logger = LoggerFactory.getLogger( NGDevServerRegistration.class );

	private NGDevServerRegistration() {}

	/**
	 * Fires a one-shot, best-effort registration of the given app name and port to the dev
	 * server. Returns immediately; the HTTP call runs on a background thread so a slow or
	 * absent dev server never delays startup.
	 */
	public static void registerInBackground( final NGApplication application, final int port ) {
		final String appName = applicationName( application );
		final String devServerPort = devServerPort( application );
		final String pid = String.valueOf( ProcessHandle.current().pid() );

		final String url = "http://localhost:" + devServerPort
				+ "/registerApp?name=" + URLEncoder.encode( appName, StandardCharsets.UTF_8 )
				+ "&port=" + port
				+ "&pid=" + pid
				+ "&runtime=ng"; // lets the dev server / a tool pick the /ng/dev/… endpoint form

		final Thread thread = new Thread( () -> ping( url, appName, port ), "NGDevServerRegistration" );
		thread.setDaemon( true );
		thread.start();
	}

	private static String devServerPort( final NGApplication application ) {
		final String configured = application.properties().get( "wolips.port" );
		return configured != null ? configured : "9485";
	}

	/**
	 * @return The name to register the application under — what a human (or agent) would call
	 *         the app, and ideally what the project is named in the IDE, since the dev server
	 *         matches the registered name against workspace project names (for dependency
	 *         resolution) and launch configurations (for stopping the app).
	 *
	 * The WOApplicationName property is the strongest signal when present. Failing that, the
	 * working directory's name — development launches (IDE, Maven, plain java from the project
	 * directory) all run with the project root as the working directory, so its name matches
	 * the IDE project. As last resorts, the application's simple class name, or — since ng
	 * application classes are conventionally all named just "Application" — the last package
	 * segment (e.g. "ng.testapp.Application" → "testapp").
	 */
	private static String applicationName( final NGApplication application ) {
		final String configured = application.properties().d().propWOApplicationName();

		if( configured != null && !configured.isEmpty() ) {
			return configured;
		}

		final Path workingDirectory = Path.of( "" ).toAbsolutePath().getFileName();

		if( workingDirectory != null && !workingDirectory.toString().isEmpty() ) {
			return workingDirectory.toString();
		}

		final String simpleName = application.getClass().getSimpleName();

		if( !"Application".equals( simpleName ) ) {
			return simpleName;
		}

		final String packageName = application.getClass().getPackageName();
		return packageName.substring( packageName.lastIndexOf( '.' ) + 1 );
	}

	private static void ping( final String url, final String appName, final int port ) {
		try {
			final HttpClient client = HttpClient.newBuilder()
					.connectTimeout( Duration.ofMillis( 500 ) )
					.build();

			final HttpRequest request = HttpRequest.newBuilder( URI.create( url ) )
					.timeout( Duration.ofSeconds( 2 ) )
					.GET()
					.build();

			final HttpResponse<Void> response = client.send( request, HttpResponse.BodyHandlers.discarding() );

			if( response.statusCode() == 200 ) {
				logger.info( "Registered '{}' (port {}) with the IDE dev server", appName, port );
			}
		}
		catch( final Exception e ) {
			// Expected and harmless when no dev server is listening (no Eclipse, CI, prod).
			// Deliberately quiet — this is a convenience, not a requirement.
		}
	}
}
