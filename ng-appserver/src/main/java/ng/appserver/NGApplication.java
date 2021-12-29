package ng.appserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.experimental.NGRouteTable;
import ng.appserver.experimental.NGRouteTable.NGRouteHandler;
import ng.appserver.wointegration.NGLifebeatThread;
import ng.appserver.wointegration.WOMPRequestHandler;

public class NGApplication {

	private static Logger logger = LoggerFactory.getLogger( NGApplication.class );

	private static NGApplication _application;

	private NGSessionStore _sessionStore;

	private NGResourceManager _resourceManager;

	private NGProperties _properties;

	public NGLifebeatThread _lifebeatThread;

	private final Map<String, NGRequestHandler> _requestHandlers = new HashMap<>();

	public void run( final String[] args, final Class<? extends NGApplication> applicationClass ) {
		final long startTime = System.currentTimeMillis();

		_properties = new NGProperties( args );

		// We need to start out with initializing logging to ensure we're seeing everything the application does during the init phase.
		initLogging( _properties.propWOOutputPath() );

		if( _properties.isDevelopmentMode() ) {
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
		logger.info( _properties._propertiesMapAsString() );

		try {
			_application = applicationClass.getDeclaredConstructor().newInstance();

			_application._resourceManager = new NGResourceManager();
			_application._sessionStore = new NGServerSessionStore();

			_application.registerRequestHandler( "wo", new NGComponentRequestHandler() );
			_application.registerRequestHandler( "wr", new NGResourceRequestHandler() );
			_application.registerRequestHandler( "wa", new NGDirectActionRequestHandler() );
			_application.registerRequestHandler( "womp", new WOMPRequestHandler() );

			_application.run();
		}
		catch( final Exception e ) {
			e.printStackTrace();
			System.exit( -1 );
		}

		if( !_properties.isDevelopmentMode() ) {
			startLifebeatThread();
		}

		logger.info( "===== Application started in {} ms at {}", (System.currentTimeMillis() - startTime), LocalDateTime.now() );
	}

	public boolean isDevelopmentMode() {
		return _properties.isDevelopmentMode();
	}

	/**
	 * Starts a lifebeat thread for communicating with wotaskd.
	 */
	private void startLifebeatThread() {
		String hostName = _properties.propWOHost();
		String appName = _properties.propWOApplicationName();
		Integer appPort = _properties.propWOPort();
		Integer lifeBeatDestinationPort = _properties.propWOLifebeatDestinationPort();
		Integer lifeBeatIntervalInSeconds = _properties.propWOLifebeatIntervalInSeconds();

		InetAddress hostAddress = null;

		try {
			hostAddress = InetAddress.getByName( hostName );
		}
		catch( final UnknownHostException e ) {
			throw new RuntimeException( "Failed to start LifebeatThread", e );
		}

		_lifebeatThread = new NGLifebeatThread( appName, appPort, hostAddress, lifeBeatDestinationPort, TimeUnit.SECONDS.convert( lifeBeatIntervalInSeconds, TimeUnit.MILLISECONDS ) );
		_lifebeatThread.setDaemon( true );
		_lifebeatThread.start();
	}

	public NGComponent pageWithName( final Class<? extends NGComponent> componentClass, NGContext context ) {
		try {
			return componentClass.getConstructor( NGContext.class ).newInstance( context );
		}
		catch( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			// FIXME: Handle the error
			throw new RuntimeException( e );
		}
	}

	public NGSessionStore sessionStore() {
		return _sessionStore;
	}

	public NGSession restoreSessionWithID( final String sessionID ) {
		return sessionStore().checkoutSessionWithID( sessionID );
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

	/**
	 * FIXME: We don't really want to return anything if this hasn't been set. Only set now for testing
	 */
	public String adaptorClassName() {
		return "ng.adaptor.jetty.NGAdaptorJetty";
	}

	private void run() {
		try {
			createAdaptor().start();
		}
		catch( final Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public static NGApplication application() {
		return _application;
	}

	public NGResourceManager resourceManager() {
		return _resourceManager;
	}

	public void registerRequestHandler( final String key, final NGRequestHandler requestHandler ) {
		_requestHandlers.put( key, requestHandler );
	}

	/**
	 * FIXME: We're currently looking for both RouteHandlers and Request handlers. These are the same thing. Consolidate // Hugi 2021-11-28
	 */
	public NGResponse dispatchRequest( final NGRequest request ) {

		logger.info( "Handling URI: " + request.uri() );

		cleanupWOURL( request );

		// FIXME: Start experimental route handling logic
		final NGRouteHandler handler = NGRouteTable.defaultRouteTable().handlerForURL( request.parsedURI() );

		if( handler != null ) {
			return handler.handle( request.parsedURI(), request.context() ).generateResponse();
		}
		// FIXME: End experimental route handling logic

		final Optional<String> requestHandlerKey = request.parsedURI().elementAt( 0 );

		// FIXME: Handle the case of no default request handler gracefully
		if( requestHandlerKey.isEmpty() ) {
			return new NGResponse( "I have no idea to handle requests without any path elements", 404 );
		}

		final NGRequestHandler requestHandler = _requestHandlers.get( requestHandlerKey.get() );

		if( requestHandler == null ) {
			return new NGResponse( "No request handler found with key " + requestHandlerKey, 404 );
		}

		return requestHandler.handleRequest( request );
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

	public NGContext createContextForRequest( NGRequest request ) {
		return new NGContext( request );
	}

	/**
	 * Redirects logging to the designated [outputPath] if set
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
	 * FIXME: This is a bit harsh. We probably want to start some sort of a graceful shutdown procedure instead of saying "'K, BYE" // Hugi 2021-11-20
	 */
	public void terminate() {
		System.exit( 0 );
	}
}