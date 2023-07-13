package ng.appserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGProperties.PropertiesSourceArgv;
import ng.appserver.NGProperties.PropertiesSourceResource;
import ng.appserver.directactions.NGDirectActionRequestHandler;
import ng.appserver.routing.NGRouteTable;
import ng.appserver.templating.NGElementUtils;
import ng.appserver.wointegration.NGDefaultLifeBeatThread;
import ng.appserver.wointegration.WOMPRequestHandler;
import x.junk.NGExceptionPage;
import x.junk.NGExceptionPageDevelopment;
import x.junk.NGSessionTimeoutPage;

public class NGApplication {

	private static Logger logger = LoggerFactory.getLogger( NGApplication.class );

	/**
	 * FIXME: This is a global NGApplication object. We don't want a global NGApplication object. It's only around for convenience for now // Hugi 2021-12-29
	 */
	@Deprecated
	private static NGApplication _application;

	/**
	 * The application's properties
	 */
	private NGProperties _properties;

	/**
	 * Session storage and coordination
	 */
	private NGSessionStore _sessionStore;

	/**
	 * Resource loading, caching and management
	 */
	private NGResourceManager _resourceManager;

	/**
	 * In the old WO world, this would have been called "requestHandlers".
	 * Since we want to have more dynamic route resolution, it makes sense to move that to a separate object.
	 */
	private List<NGRouteTable> _routeTables = new ArrayList<>();

	/**
	 * FIXME: Temporary placeholder while we figure out the perfect initialization process // Hugi 2022-10-22
	 */
	public static void run( final String[] args, final Class<? extends NGApplication> applicationClass ) {
		runAndReturn( args, applicationClass );
	}

	/**
	 * FIXME: Initialization still feels a little weird, while we're moving away from the way it's handled in WOApplication. Look a little more into the flow of application initialization // Hugi 2021-12-29
	 */
	public static <E extends NGApplication> E runAndReturn( final String[] args, final Class<E> applicationClass ) {
		final long startTime = System.currentTimeMillis();

		final NGProperties properties = new NGProperties();
		properties.addAndReadResourceSource( new PropertiesSourceResource( "Properties" ) );
		properties.addAndReadResourceSource( new PropertiesSourceArgv( args ) );

		// We need to start out with initializing logging to ensure we're seeing everything the application does during the init phase.
		redirectOutputToFilesIfOutputPathSet( properties.propWOOutputPath() );

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

		logger.info( "===== Properties =====\n" + properties._propertiesMapAsString() );

		NGApplication application = null;

		try {
			application = applicationClass.getDeclaredConstructor().newInstance();

			// FIXME: This is just plain wrong. We want properties to be accessible during application initialization. Here we're loading properties after construction
			application._properties = properties;

			// FIXME: We also might want to be more explicit about this
			application.start();

			if( properties.propWOLifebeatEnabled() ) {
				NGDefaultLifeBeatThread.start( application._properties );
			}

			logger.info( "===== Application started in {} ms at {}", (System.currentTimeMillis() - startTime), LocalDateTime.now() );

			// Assigning that unwanted global application...
			_application = application;

			return (E)application;
		}
		catch( final Exception e ) {
			// FIXME: we're going to want to identify certain error conditions an respond to them with an explanation // Hugi 2022-10-22
			e.printStackTrace();
			System.exit( -1 );

			// Essentially a dead return, just to satisfy the java compiler (which isn't aware that it was just violently stabbed to death using System.exit())
			return null;
		}
	}

	/**
	 * Construct an application with no properties (and no property loading)
	 */
	public NGApplication() {
		_resourceManager = new NGResourceManager();
		_sessionStore = new NGServerSessionStore();

		// The first table in the list is the "user route table"
		_routeTables.add( new NGRouteTable( "User routes" ) );

		// Then we add the "system route table"
		final NGRouteTable systemRoutes = new NGRouteTable( "System routes" );
		systemRoutes.map( NGComponentRequestHandler.DEFAULT_PATH, new NGComponentRequestHandler() );
		systemRoutes.map( NGResourceRequestHandler.DEFAULT_PATH, new NGResourceRequestHandler() );
		systemRoutes.map( "/wd/", new NGResourceRequestHandlerDynamic() );
		systemRoutes.map( "/wa/", new NGDirectActionRequestHandler() );
		systemRoutes.map( "/womp/", new WOMPRequestHandler() );
		systemRoutes.map( "/sessionCookieReset/", ( request ) -> {
			final NGResponse response = new NGResponse();

			response.setHeader( "location", "/" );
			response.setStatus( 302 );
			response.setHeader( "content-type", "text/html" );
			response.setHeader( "content-length", "0" );
			response.addCookie( createSessionCookie( "SessionCookieKillerCookieValuesDoesNotMatter", 0 ) );

			return response;
		} );
		_routeTables.add( systemRoutes );
	}

	/**
	 * Starts the adaptor
	 */
	private void start() {
		try {
			createAdaptor().start( this );
		}
		catch( final Exception e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * @return The class to use when constructing a new session. By default we look for a class named "Session" in the same package as the application class
	 */
	protected Class<? extends NGSession> _sessionClass() {
		try {
			return (Class<? extends NGSession>)Class.forName( getClass().getPackageName() + ".Session" );
		}
		catch( ClassNotFoundException e ) {
			logger.info( "Custom session class not found. Defaulting to " + NGSession.class.getName() );
			return NGSession.class;
		}
	}

	/**
	 * @return The fully qualified class name of the http adaptor
	 */
	public String adaptorClassName() {
		return "ng.adaptor.jetty.NGAdaptorJetty";
		// FIXME: This should eventually return the name of our own adaptor. Using Jetty for now (since it's easier to implement) // Hugi 2021-12-29
		// return ng.adaptor.raw.NGAdaptorRaw.class.getName();
	}

	/**
	 * @return An adaptor class instance
	 */
	private NGAdaptor createAdaptor() {
		try {
			final Class<? extends NGAdaptor> adaptorClass = (Class<? extends NGAdaptor>)Class.forName( adaptorClassName() );
			return adaptorClass.getConstructor().newInstance();
		}
		catch( Exception e ) {
			logger.error( "Failed to instantiate adaptor class: " + adaptorClassName(), e );
			e.printStackTrace();
			System.exit( -1 );

			// Essentially a dead return, just to satisfy the java compiler (which isn't aware that it was just violently stabbed to death using System.exit())
			return null;
		}
	}

	public NGProperties properties() {
		return _properties;
	}

	/**
	 * @return The default route table.
	 */
	public NGRouteTable routeTable() {
		return _routeTables.get( 0 );
	}

	/**
	 * @return a request handler for the given route, by searching all route tables
	 *
	 * FIXME: This belongs in a routing related class // Hugi 2022-10-16
	 */
	public NGRequestHandler handlerForURL( String url ) {
		for( NGRouteTable routeTable : _routeTables ) {
			final NGRequestHandler handler = routeTable.handlerForURL( url );

			if( handler != null ) {
				logger.debug( "Matched URL '{}' with route '{}' from table '{}'", url, "[untitled]", routeTable.name() ); // FIXME: Missing route name instead of [untitled] // Hugi 2022-11-27
				return handler;
			}
		}

		return null;
	}

	/**
	 * FIXME: I'm not quite sure what to do about this variable. Belongs here or someplace else? // Hugi 2023-03-10
	 */
	public boolean isDevelopmentMode() {
		return _properties.isDevelopmentMode();
	}

	/**
	 * @return The named component, where [componentName] can be either the component's simple class name or full class name.
	 */
	public NGComponent pageWithName( final String componentName, final NGContext context ) {
		Objects.requireNonNull( componentName, "'componentName' must not be null. I can't create components from nothing." );
		Objects.requireNonNull( context, "'context' must not be null. What's life without context?" );

		final NGComponentDefinition definition = _componentDefinition( componentName, Collections.emptyList() );
		return pageWithName( definition, context );
	}

	/**
	 * @return A new instance of [componentClass] in the given [context]
	 */
	public <E extends NGComponent> E pageWithName( final Class<E> componentClass, final NGContext context ) {
		Objects.requireNonNull( componentClass, "'componentClass' must not be null. I can't create components from nothing." );
		Objects.requireNonNull( context, "'context' must not be null. What's life without context?" );

		final NGComponentDefinition definition = _componentDefinition( componentClass, Collections.emptyList() );
		return (E)pageWithName( definition, context );
	}

	/**
	 * @return A new instance of [componentDefinition] in the given [context]
	 */
	private NGComponent pageWithName( final NGComponentDefinition componentDefinition, final NGContext context ) {
		Objects.requireNonNull( componentDefinition );
		Objects.requireNonNull( context );

		return componentDefinition.componentInstanceInContext( context );
	}

	/**
	 * @return The global NGApplication instance.
	 */
	@Deprecated
	public static NGApplication application() {
		return _application;
	}

	/**
	 * @return true if we want to enable caches
	 *
	 * FIXME: This is not here to stay. It's just nice to have a single location to refer to for now, rather than always using isDevelopmentMode() // Hugi 2022-10-19
	 * FIXME: While this is temporary, note that it's not best for performance to check properties every time this method is invoked. We should be caching the result. Just not doing it now, since we're still consolidating caching // Hugi 2023-03-10
	 */
	@Deprecated
	public boolean cachingEnabled() {
		return !isDevelopmentMode();
	}

	public NGResourceManager resourceManager() {
		return _resourceManager;
	}

	public NGSessionStore sessionStore() {
		return _sessionStore;
	}

	public NGResponse dispatchRequest( final NGRequest request ) {

		try {
			cleanupWOURL( request );

			final NGResponse response;

			// FIXME: Handle the case of no default request handler gracefully // Hugi 2021-12-29
			if( request.parsedURI().length() == 0 ) {
				response = defaultResponse( request ).generateResponse();
			}
			else {
				final NGRequestHandler requestHandler = handlerForURL( request.uri() );

				if( requestHandler == null ) {
					// FIXME: Very, very experimental public resource handler.
					final String resourcePath = request.uri();

					if( resourcePath.isEmpty() ) {
						return new NGResponse( "No resource name specified", 400 );
					}

					// FIXME: We want this to work with streams, not byte arrays.
					// To make this work, we'll have to cache a wrapper class for the resource; that wrapper must give us a "stream provider", not an actual stream, since we'll be consuming the stream of a cached resource multiple times.
					// Hugi 2023-02-17
					final Optional<byte[]> resourceBytes = resourceManager().bytesForPublicResourceNamed( resourcePath );

					return NGResourceRequestHandler.responseForResource( resourceBytes, resourcePath );

					// return new NGResponse( "No request handler found for uri " + request.uri(), 404 );
				}

				response = requestHandler.handleRequest( request );

				if( response == null ) {
					throw new NullPointerException( String.format( "'%s' returned a null response. That's just rude.", requestHandler.getClass().getName() ) );
				}
			}

			// FIXME: Doesn't feel like the place to set the session ID in the response, but let's do it anyway :D // Hugi 2023-01-10
			final String sessionID = request._sessionID();

			if( sessionID != null ) {
				if( request.existingSession() != null ) { // FIXME: existingSession() isn't really a reliable way to get the session (at least not yet)  // Hugi 2023-01-11
					response.addCookie( createSessionCookie( sessionID, (int)request.existingSession().timeOut().toSeconds() ) );
				}
			}

			return response;
		}
		catch( final NGSessionRestorationException e ) {
			return handleSessionRestorationException( e ).generateResponse();
		}
		catch( final NGPageRestorationException e ) {
			return handlePageRestorationException( e ).generateResponse();
		}
		catch( final Throwable throwable ) {
			handleException( throwable );
			return exceptionResponse( throwable, request.context() ).generateResponse();
		}
	}

	private static NGCookie createSessionCookie( final String sessionID, final int maxAge ) {
		final NGCookie sessionCookie = new NGCookie( NGRequest.SESSION_ID_COOKIE_NAME, sessionID );
		sessionCookie.setMaxAge( maxAge );
		sessionCookie.setPath( "/" ); // FIXME: We probably want this to be configurable // Hugi 2023-02-06
		// sessionCookie.setDomain( sessionID ) // FIXME: Implement // Hugi 2023-01-11
		// sessionCookie.setSameSite( "Strict" ) // FIXME: Add once we have Servlet API 6 // Hugi 2023-02-06
		// sessionCookie.setSecure( ... ) // FIXME: We also might want this to be configurable... Sending session cookies over HTTP isn't exactly brilliant in a production setting // Hugi 2023-02-06
		return sessionCookie;
	}

	/**
	 * Handle a Request/Response loop occurring throwable before generating a response for it
	 */
	protected void handleException( Throwable throwable ) {
		throwable.printStackTrace();
	}

	/**
	 * @return A newly created session for the given NGRequest
	 *
	 * If you need to catch some info about the user, and do something like, for example, automatically log in a user based on a cookie value, this would be just the place.
	 */
	public NGSession createSessionForRequest( NGRequest request ) {
		try {
			return _sessionClass().getConstructor().newInstance();
		}
		catch( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * If the application fails to restore a session during request handling, this method will be invoked to generate a response for the user
	 *
	 * @return The page to return to the user when a session restoration error occurs.
	 */
	protected NGActionResults handleSessionRestorationException( final NGSessionRestorationException exception ) {
		final NGSessionTimeoutPage nextPage = pageWithName( NGSessionTimeoutPage.class, exception.request().context() ); // FIXME: Working with a context within a dead session feels weird // Hugi 2023-01-11
		nextPage.setException( exception );
		return nextPage;
	}

	/**
	 * If the application fails to restore a page from the session's page cache during component action request handling,
	 * (usually because the page cache has been exhausted, and the page pushed out of the cache), this method will be invoked and it's response returned to the user.
	 *
	 * FIXME: Create a nicer response for this // Hugi 2023-02-10
	 * FIXME: This is the component action request handler leaking into the generic application // Hugi 2023-07-01
	 */
	protected NGActionResults handlePageRestorationException( final NGPageRestorationException exception ) {
		return new NGResponse( exception.getMessage(), 404 );
	}

	/**
	 * @return The response generated when an exception occurs
	 */
	public NGActionResults exceptionResponse( final Throwable throwable, final NGContext context ) {

		final boolean isDevelopmentMode = isDevelopmentMode();

		// If we're in development mode, we want to show some extra nice debugging information (sources, caches, context info etc.)
		if( isDevelopmentMode ) {
			final NGExceptionPageDevelopment nextPage = pageWithName( NGExceptionPageDevelopment.class, context );
			nextPage.setException( throwable );
			return nextPage;
		}

		final NGExceptionPage nextPage = pageWithName( NGExceptionPage.class, context );
		nextPage.setException( throwable );
		return nextPage;
	}

	/**
	 * @return A default response for requests to the root.
	 */
	public NGActionResults defaultResponse( final NGRequest request ) {
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
	private void cleanupWOURL( final NGRequest request ) {

		final Pattern pattern = Pattern.compile( "^/(cgi-bin|Apps)/WebObjects/" + properties().propWOApplicationName() + ".woa(/[0-9])?" );
		final Matcher matcher = pattern.matcher( request.uri() );

		if( matcher.find() ) {
			request.setURI( request.uri().substring( matcher.group().length() ) );
			logger.info( "Rewrote WO URI to {}", request.uri() );
		}

		/*
		String woStart = "/Apps/WebObjects/%s.woa/1".formatted( properties().propWOApplicationName() );
		
		if( request.uri().startsWith( woStart ) ) {
			request.setURI( request.uri().substring( woStart.length() ) );
			logger.info( "Rewrote WO URI to {}", request.uri() );
		}
		
		woStart = "/cgi-bin/WebObjects/%s.woa".formatted( properties().propWOApplicationName() );
		
		if( request.uri().startsWith( woStart ) ) {
			request.setURI( request.uri().substring( woStart.length() ) );
			logger.info( "Rewrote WO URI to {}", request.uri() );
		}
		*/
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
	private static void redirectOutputToFilesIfOutputPathSet( final String outputPath ) {
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

	/**
	 * @return The componentDefinition corresponding to the given NGComponent class.
	 *
	 * FIXME: Languages are currently not supported, but the gets included while we ponder design for that // Hugi 2023-04-14
	 * FIXME: This should not be static // Hugi 2023-04-14
	 */
	private static NGComponentDefinition _componentDefinition( final Class<? extends NGComponent> componentClass, final List<String> languages ) {
		Objects.requireNonNull( componentClass );
		Objects.requireNonNull( languages );

		return NGComponentDefinition.get( componentClass );
	}

	/**
	 * @return The componentDefinition corresponding to the named NGComponent
	 *
	 * FIXME: Languages are currently not supported, but the gets included while we ponder design for that // Hugi 2023-04-14
	 * FIXME: This should not be static // Hugi 2023-04-14
	 */
	public static NGComponentDefinition _componentDefinition( final String componentName, final List<String> languages ) {
		Objects.requireNonNull( componentName );
		Objects.requireNonNull( languages );

		return NGComponentDefinition.get( componentName );
	}

	/**
	 * FIXME: Languages are currently not supported, but the gets included while we ponder design for that // Hugi 2023-04-14
	 * FIXME: This should not be static // Hugi 2023-04-14
	 * CHECKME: Since this can only ever return DynamicElements, we can probably narrow the return type here // Hugi 2023-05-07
	 *
	 * @param name The name identifying what element we're getting
	 * @param associations Associations used to bind the generated element to it's parent
	 * @param contentTemplate The content wrapped by the element (if a container element)
	 * @param languages A list of languages you'd prefer, in order of most preferred to least preferred
	 *
	 * @return An instance of the named dynamic element. This can be a classless component (in which case it's the template name), a simple class name or a full class name
	 */
	public static NGElement dynamicElementWithName( final String identifier, final Map<String, NGAssociation> associations, final NGElement contentTemplate, final List<String> languages ) {
		Objects.requireNonNull( identifier );
		Objects.requireNonNull( associations );
		Objects.requireNonNull( languages );

		final String name;

		// First we're going to check if we have a tag alias present
		final String shortName = NGElementUtils.tagShortcutMap().get( identifier );

		if( shortName != null ) {
			name = shortName;
		}
		else {
			// If no shortcut is present for the given identifier, use the original identifier
			name = identifier;
		}

		// First we locate the class of the element we're going to render.
		final Class<? extends NGElement> elementClass = NGElementUtils.classWithNameNullIfNotFound( name );

		// If we don't find a class for the element, we're going to try going down the route of a classless component.
		if( elementClass == null ) {
			final NGComponentDefinition componentDefinition = _componentDefinition( name, languages );
			return componentDefinition.componentReferenceWithAssociations( associations, contentTemplate );
		}

		// First we check if this is a dynamic element
		if( NGDynamicElement.class.isAssignableFrom( elementClass ) ) {
			return NGElementUtils.createElement( elementClass, name, associations, contentTemplate );
		}

		// If it's not an element, let's move on to creating a component reference instead
		if( NGComponent.class.isAssignableFrom( elementClass ) ) {
			final NGComponentDefinition componentDefinition = _componentDefinition( (Class<? extends NGComponent>)elementClass, languages );
			return componentDefinition.componentReferenceWithAssociations( associations, contentTemplate );
		}

		// We should never end up here unless we got an incorrect/non-existent element name
		throw new IllegalArgumentException( "I could not construct a dynamic element named '%s'".formatted( name ) );
	}
}