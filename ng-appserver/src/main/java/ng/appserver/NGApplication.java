package ng.appserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.directactions.NGDirectActionRequestHandler;
import ng.appserver.experimental.NGRouteTable;
import ng.appserver.wointegration.NGLifebeatThread;
import ng.appserver.wointegration.WOMPRequestHandler;

public class NGApplication {

	private static Logger logger = LoggerFactory.getLogger( NGApplication.class );

	/**
	 * FIXME: This is a global NGApplication object. We don't want a global NGApplication object // Hugi 2021-12-29
	 */
	private static NGApplication _application;

	private NGProperties _properties;

	private NGSessionStore _sessionStore;

	private NGResourceManager _resourceManager;

	/**
	 * In the old WO world, this would have been called "requestHandlers".
	 * Since we want to have more dynamic route resolution, it makes sense to move that to a separate object.
	 */
	private NGRouteTable _routeTable = new NGRouteTable();

	/**
	 * FIXME: public for the benefit of WOMPRequestHandler, which uses it to generate messages to send to wotaskd. Let's look into that // Hugi 2021-12-29
	 */
	public NGLifebeatThread _lifebeatThread;

	/**
	 * FIXME: Initialization still feels a little weird, while we're moving away from the way it's handled in WOApplication. Look a little more into the flow of application initialization // Hugi 2021-12-29
	 */
	public static void run( final String[] args, final Class<? extends NGApplication> applicationClass ) {
		final long startTime = System.currentTimeMillis();

		NGProperties properties = new NGProperties( args );

		// We need to start out with initializing logging to ensure we're seeing everything the application does during the init phase.
		initLogging( properties.propWOOutputPath() );

		if( properties.isDevelopmentMode() ) {
			logger.info( "========================================" );
			logger.info( "===== Running in development mode! =====" );
			logger.info( "========================================" );
		}
		else {
			logger.info( "=======================================" );
			logger.info( "===== Running in production mode! =====" );
			logger.info( "=======================================" );
		}

		logger.info( "===== Parsed properties" );
		logger.info( properties._propertiesMapAsString() );

		try {
			_application = applicationClass.getDeclaredConstructor().newInstance();

			_application._resourceManager = new NGResourceManager();
			_application._sessionStore = new NGServerSessionStore();
			_application._properties = properties;

			_application._routeTable.map( "/wo/", new NGComponentRequestHandler() );
			_application._routeTable.map( "/wr/", new NGResourceRequestHandler() );
			_application._routeTable.map( "/wa/", new NGDirectActionRequestHandler() );
			_application._routeTable.map( "/womp/", new WOMPRequestHandler() );

			_application.run();
		}
		catch( final Exception e ) {
			e.printStackTrace();
			System.exit( -1 );
		}

		if( properties.propWOLifebeatEnabled() ) {
			_application.startLifebeatThread();
		}

		logger.info( "===== Application started in {} ms at {}", (System.currentTimeMillis() - startTime), LocalDateTime.now() );
	}

	private void run() {
		try {
			createAdaptor().start();
		}
		catch( final Exception e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * FIXME: This should eventually return the name of our own adaptor. Using Jetty for now (since it's easier to implement) // Hugi 2021-12-29
	 */
	public String adaptorClassName() {
		return "ng.adaptor.jetty.NGAdaptorJetty";
	}

	private NGAdaptor createAdaptor() {
		try {
			final Class<? extends NGAdaptor> adaptorClass = (Class<? extends NGAdaptor>)Class.forName( adaptorClassName() );
			return adaptorClass.getConstructor().newInstance();
		}
		catch( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e ) {
			// FIXME: Handle the error
			e.printStackTrace();
			System.exit( -1 );
			return null; // wat?
		}
	}

	public NGProperties properties() {
		return _properties;
	}

	public NGRouteTable routeTable() {
		return _routeTable;
	}

	/**
	 * @return A new instance of [componentClass] in the given [context]
	 *
	 * FIXME: Are components really a part of the basic framework? If so; does component construction really belong in NGApplication // Hugi 2021-12-29
	 */
	public NGComponent pageWithName( final Class<? extends NGComponent> componentClass, final NGContext context ) {
		final NGComponentDefinition definition = _componentDefinition( componentClass );

		if( definition == null ) {
			throw new RuntimeException( "No such component definition: " + componentClass );
		}

		return definition.componentInstanceInstanceInContext( componentClass, context );
	}

	private NGComponentDefinition _componentDefinition( Class<? extends NGComponent> componentClass ) {
		return null;
	}

	public static NGApplication application() {
		return _application;
	}

	public NGResourceManager resourceManager() {
		return _resourceManager;
	}

	public NGSessionStore sessionStore() {
		return _sessionStore;
	}

	public NGSession restoreSessionWithID( final String sessionID ) {
		return sessionStore().checkoutSessionWithID( sessionID );
	}

	public NGResponse dispatchRequest( final NGRequest request ) {

		logger.info( "Handling URI: " + request.uri() );

		cleanupWOURL( request );

		// FIXME: Handle the case of no default request handler gracefully // Hugi 2021-12-29
		if( request.parsedURI().length() == 0 ) {
			return defaultResponse( request );
		}

		final NGRequestHandler handler = _routeTable.handlerForURL( request.uri() );

		if( handler == null ) {
			return new NGResponse( "No request handler found for uri " + request.uri(), 404 );
		}

		return handler.handleRequest( request );
	}

	/**
	 * @return A default response for requests to the root.
	 *
	 *  FIXME: This is just here as a temporary placeholder until we decide on a nicer default request handling mechanism
	 */
	private static NGResponse defaultResponse( final NGRequest request ) {
		NGResponse response = new NGResponse( "Welcome to NGObjects!\nSorry, but I'm young and I still have no idea how to handle the default request", 404 );
		response.appendContentString( "\n\nWould you like to see your request headers instead?\n\n" );

		for( Entry<String, List<String>> header : request.headers().entrySet() ) {
			response.appendContentString( header.getKey() + " : " + header.getValue() );
			response.appendContentString( "\n" );
		}

		return response;
	}

	/**
	 * FIXME: Well this is horrid // Hugi 2021-11-20
	 *
	 * What we're doing here is allowing for the WO URL structure, which is somewhat required to work with the WO Apache Adaptor.
	 * Ideally, we don't want to prefix URLs at all, instead just handling requests at root level. But to begin with, perhaps we can
	 * just allow for certain "prefix patterns" to mask out the WO part of the URL and hide it from the app. It might even be a useful
	 * little feature on it's own.
	 */
	private static void cleanupWOURL( final NGRequest request ) {
		String woStart = "/Apps/WebObjects/Rebelliant.woa/1";

		if( request.uri().startsWith( woStart ) ) {
			request.setURI( request.uri().substring( woStart.length() ) );
			logger.info( "Rewrote WO URI to {}", request.uri() );
		}

		woStart = "/cgi-bin/WebObjects/Rebelliant.woa";

		if( request.uri().startsWith( woStart ) ) {
			request.setURI( request.uri().substring( woStart.length() ) );
			logger.info( "Rewrote WO URI to {}", request.uri() );
		}
	}

	/**
	 * FIXME: Like the other "create..." methods, this one is inspired by WO. It's really a relic from the time when WOApplication served as The Central Thing Of All Things That Are.
	 * Good idea at the time, it made Wonder possible… But it's really just an older type of a factory or, well, dependency injection. Not sure we want to keep this way of constructing objects. // Hugi 2021-12-29
	 */
	public NGContext createContextForRequest( NGRequest request ) {
		return new NGContext( request );
	}

	/**
	 * Redirects logging to the designated [outputPath] if set.
	 *
	 * If a file exists at the given path, it is renamed by adding the current date to it's name.
	 * Pretty much the same way WOOutputPath is handled in WO.
	 */
	private static void initLogging( final String outputPath ) {
		if( outputPath != null ) {
			// Archive the older logFile if it exists
			final File outputFile = new File( outputPath );

			if( outputFile.exists() ) {
				final File oldOutputFile = new File( outputPath + "." + LocalDateTime.now() );
				outputFile.renameTo( oldOutputFile );
			}

			try {
				final PrintStream out = new PrintStream( new FileOutputStream( outputPath ) );
				System.setOut( out );
				System.setErr( out );
				logger.info( "Redirected System.out and System.err to {}", outputPath );
			}
			catch( final FileNotFoundException e ) {
				throw new RuntimeException( e );
			}
		}
		else {
			logger.info( "OutputPath not set. Using standard System.out and System.err" );
		}
	}

	/**
	 * Starts a lifebeat thread for communicating with wotaskd.
	 */
	private void startLifebeatThread() {
		final String hostName = _properties.propWOHost();
		final String appName = _properties.propWOApplicationName();
		final Integer appPort = _properties.propWOPort();
		final Integer lifeBeatDestinationPort = _properties.propWOLifebeatDestinationPort();
		final Integer lifeBeatIntervalInSeconds = _properties.propWOLifebeatIntervalInSeconds();
		final long lifeBeatIntervalInMilliseconds = TimeUnit.MILLISECONDS.convert( lifeBeatIntervalInSeconds, TimeUnit.SECONDS );

		InetAddress hostAddress = null;

		try {
			hostAddress = InetAddress.getByName( hostName );
		}
		catch( final UnknownHostException e ) {
			throw new RuntimeException( "Failed to start LifebeatThread", e );
		}

		_lifebeatThread = new NGLifebeatThread( appName, appPort, hostAddress, lifeBeatDestinationPort, lifeBeatIntervalInMilliseconds );
		_lifebeatThread.setDaemon( true );
		_lifebeatThread.start();
	}

	/**
	 * FIXME: This is a bit harsh. We probably want to start some sort of a graceful shutdown procedure instead of saying "'K, BYE" // Hugi 2021-11-20
	 */
	public void terminate() {
		System.exit( 0 );
	}
}