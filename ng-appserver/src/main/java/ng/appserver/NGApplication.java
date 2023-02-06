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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	 * FIXME: This is a global NGApplication object. We don't want a global NGApplication object // Hugi 2021-12-29
	 */
	@Deprecated
	private static NGApplication _application;

	/**
	 * The application's properties
	 *
	 * FIXME: While having properties is useful, we should be wondering if they should be public or only for internal use // Hugi 2022-10-22
	 */
	private NGProperties _properties;

	/**
	 * Session storage and coordination
	 *
	 * FIXME: A question of if this is a part of the application. In the case of stateful actions, it's likely that it'll have to be // Hugi 2022-10-22
	 */
	private NGSessionStore _sessionStore;

	/**
	 * Resource loading, caching and management
	 *
	 * FIXME: A question of if this is a part of the application or if resource loading is a separate "thing" // Hugi 2022-10-22
	 */
	private NGResourceManager _resourceManager;

	/**
	 * In the old WO world, this would have been called "requestHandlers".
	 * Since we want to have more dynamic route resolution, it makes sense to move that to a separate object.
	 */
	private List<NGRouteTable> _routeTables = new ArrayList<>();

	/**
	 * FIXME: Temporary placeholder method while we continue to figure out the eprfect initialization process // Hugi 2022-10-22
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
		properties.putAll( NGProperties.loadDefaultProperties() );
		properties.putAll( NGProperties.propertiesFromArgsString( args ) );

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

			// FIXME: Assigning that unwanted global application...
			_application = application;

			return (E)application;
		}
		catch( final Exception e ) {
			// FIXME: we're going to want to identify certain error conditions an respond to them with an explanation // Hugi 2022-10-22
			e.printStackTrace();
			System.exit( -1 );

			// Essentially a dead return, just to satisfy the java compiler (which doesn't seem aware that it was just violently stabbed to death using System.exit())
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
		systemRoutes.map( "/wo/", new NGComponentRequestHandler() );
		systemRoutes.map( "/wr/", new NGResourceRequestHandler() );
		systemRoutes.map( "/wd/", new NGResourceRequestHandlerDynamic() );
		systemRoutes.map( "/wa/", new NGDirectActionRequestHandler() );
		systemRoutes.map( "/womp/", new WOMPRequestHandler() );
		systemRoutes.map( "/sessionCookieReset/", ( request ) -> {
			final NGResponse response = new NGResponse( "<p>Session cookie reset</p><p><a href=\"/\">Re-enter</a></p>", 200 );
			final NGCookie sessionCookie = new NGCookie( NGRequest.SESSION_ID_COOKIE_NAME, "ded" );
			sessionCookie.setMaxAge( 0 );
			sessionCookie.setPath( "/" );
			//				sessionCookie.setDomain( sessionID ) // FIXME: Implement
			response.addCookie( sessionCookie );
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
	 * FIXME: This should eventually return the name of our own adaptor. Using Jetty for now (since it's easier to implement) // Hugi 2021-12-29
	 */
	public String adaptorClassName() {
		return "ng.adaptor.jetty.NGAdaptorJetty";
		//		return ng.adaptor.raw.NGAdaptorRaw.class.getName();
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
				logger.info( "Matched URL '{}' with route '{}' from table '{}'", url, "[untitled]", routeTable.name() ); // FIXME: Missing route name instead of [untitled] // Hugi 2022-11-27
				return handler;
			}
		}

		return null;
	}

	/**
	 * FIXME: I'm not quite sure what to do about this variable. Belongs here or someplace else?
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

		// FIXME: componentDefinition will probably never be null // Hugi 2022-10-10
		if( componentDefinition == null ) {
			throw new RuntimeException( "No such component definition: " + componentDefinition );
		}

		final NGComponent componentInstance = componentDefinition.componentInstanceInContext( context );

		// Allow the context to keep track of the actual page
		//		context.setPage( componentInstance );

		// Let the context know that this is the component we're currently rendering
		//		context.setCurrentComponent( componentInstance );

		// FIXME: This is horrible, but we're using it for experimentation
		// At least the idea is that here we're catching the elementID of the element that invoked the action and trying to save under that key
		//		NGComponentRequestHandler.savePage( NGComponentRequestHandler.pageCacheKey( context.contextID(), null ), componentInstance );

		return componentInstance;
	}

	/**
	 * @return The global NGApplication instance.
	 *
	 * FIXME: I really do not want a global instance in the future, but I'm keeping it around for now as it's comforting while working with familiar patterns. // Hugi 2022-10-19
	 */
	@Deprecated
	public static NGApplication application() {
		return _application;
	}

	/**
	 * @return true if we want to enable caches
	 *
	 * FIXME: This is not here to stay. It's just nice to have a single location to refer to for now, rather than always using isDevelopmentMode() // Hugi 2022-10-19
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

	public NGSession restoreSessionWithID( final String sessionID ) {
		return sessionStore().checkoutSessionWithID( sessionID );
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
					return new NGResponse( "No request handler found for uri " + request.uri(), 404 );
				}

				response = requestHandler.handleRequest( request );

				if( response == null ) {
					throw new NullPointerException( String.format( "'%s' returned a null response. That's just rude.", requestHandler.getClass().getName() ) );
				}
			}

			// FIXME: Doesn't feel like the place to set the session ID in the response, but let's do it anyway :D // Hugi 2023-01-10
			final String sessionID = request._sessionID();

			if( sessionID != null ) {
				if( request.existingSession() != null ) { // FIXME: Yuck // Hugi 2023-01-11
					final NGCookie sessionCookie = new NGCookie( NGRequest.SESSION_ID_COOKIE_NAME, sessionID );
					sessionCookie.setMaxAge( (int)request.existingSession().timeOut().toSeconds() ); // FIXME: Optimally, we wouldn't access the session object just to get the timeout value // Hugi 2023-01-11
					sessionCookie.setPath( "/" ); // FIXME: We probably want this to be configurable // Hugi 2023-02-06
					// sessionCookie.setDomain( sessionID ) // FIXME: Implement // Hugi 2023-01-11
					// sessionCookie.setSameSite( "Strict" ) // FIXME: Add once we have Servlet API 6 // Hugi 2023-02-06
					// sessionCookie.setSecure( ... ) // FIXME: We also might want this to be configurable... Sending session cookies over HTTP isn't exactly brilliant in a production setting // Hugi 2023-02-06
					response.addCookie( sessionCookie );
				}
			}

			return response;
		}
		catch( NGSessionRestorationException e ) {
			// FIXME: Some debugging logic for good measure // Hugi 2023-01-11
			System.out.println( "====== START Session restoration error location =====" );
			e.printStackTrace(); // FIXME
			System.out.println( "====== END Session restoration error location =====" );
			return handleSessionRestorationException( e ).generateResponse();
		}
		catch( Throwable throwable ) {
			// FIXME: Generate a uniqueID for the exception that occurred and show it to the user (for tracing/debugging) // Hugi 2022-10-13
			handleException( throwable );
			return exceptionResponse( throwable, request.context() ).generateResponse();
		}
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
			// FIXME: I can't just keep on throwing RuntimeException everywhere // Hugi 2023-02-05
			throw new RuntimeException( e );
		}
	}

	/**
	 * If the application fails to restore a session during request handling, this method will be invoked to generate a response for the user
	 *
	 * @return The page to return to the user when a session restoration error occurs.
	 *
	 * FIXME: Should this maybe just be an optional branch in the generic exception handling? // Hugi 2023-01-11
	 */
	protected NGActionResults handleSessionRestorationException( final NGSessionRestorationException exception ) {
		//		return new NGResponse( "Session expired", 200 ); // FIXME: A raw, non-component baesed error might still be a good idea? // Hugi 2023-01-11
		final NGSessionTimeoutPage nextPage = pageWithName( NGSessionTimeoutPage.class, exception.request().context() ); // FIXME: Working with a context withing a dead session feels weird // Hugi 2023-01-11
		nextPage.setException( exception );
		return nextPage;
	}

	/**
	 * @return The response generated when an exception occurs
	 */
	public NGActionResults exceptionResponse( final Throwable throwable, final NGContext context ) {

		// FIXME: Link up the production exception page // Hugi 2022-04-20
		boolean isDevelopmentMode = true; // isDevelopmentMode();

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
	 * @return A  response generated when an exception occurs
	 *
	 * FIXME: I'm letting this be for now, while we mull over if we wants thing to work without components/templating // Hugi 2022-10-08
	 */
	@Deprecated
	private NGActionResults rawExceptionResponse( final Throwable throwable, final NGContext context ) {
		final StringBuilder b = new StringBuilder();
		b.append( "<style>body{ font-family: sans-serif}</style>" );
		b.append( "<h3>An exception occurred</h3>" );
		b.append( "<h1>%s</h1>".formatted( throwable.getClass().getName() ) );
		b.append( "<h2>%s</h2>".formatted( throwable.getMessage() ) );

		if( throwable.getCause() != null ) {
			b.append( "<h3>Cause: %s</h3>".formatted( throwable.getCause().getMessage() ) );
		}

		for( StackTraceElement ste : throwable.getStackTrace() ) {
			final String packageNameOnly = ste.getClassName().substring( 0, ste.getClassName().lastIndexOf( "." ) );
			final String simpleClassNameOnly = ste.getClassName().substring( ste.getClassName().lastIndexOf( "." ) + 1 );

			b.append( "<span style=\"display: inline-block; min-width: 300px\">%s</span>".formatted( packageNameOnly ) );
			b.append( "<span style=\"display: inline-block; min-width: 500px\">%s</span>".formatted( simpleClassNameOnly + "." + ste.getMethodName() + "()" ) );
			b.append( ste.getFileName() + ":" + ste.getLineNumber() );
			b.append( "<br>" );
		}

		return new NGResponse( b.toString(), 500 );
	}

	/**
	 * @return A default response for requests to the root.
	 *
	 *  FIXME: This is just here as a temporary placeholder until we decide on a nicer default request handling mechanism
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
	 * @return The componentDefinition corresponding to the given WOComponent class.
	 *
	 * FIXME: This is currently extremely simplistic. We need to check for the existence of a definition, add localization etc. // Hugi 2022-01-16
	 * FIXME: This should not be static, belongs in an instance of a different class.
	 */
	private static NGComponentDefinition _componentDefinition( final Class<? extends NGComponent> componentClass, final List<String> languages ) {
		Objects.requireNonNull( componentClass );
		Objects.requireNonNull( languages );

		return NGComponentDefinition.get( componentClass );
	}

	/**
	 * @return The componentDefinition corresponding to the named WOComponent
	 *
	 * FIXME: Languages aren't supported either yet, but I'm including the parameter while I consider what to do about it.
	 * FIXME: This should not be static, belongs in an instance of a different class.
	 */
	public static NGComponentDefinition _componentDefinition( final String componentName, final List<String> languages ) {
		Objects.requireNonNull( componentName );
		Objects.requireNonNull( languages );

		return NGComponentDefinition.get( componentName );
	}

	/**
	 * FIXME: This should not be static, belongs in an instance of a different class.
	 * FIXME: If we're going to introduce namespaces, this would be the place
	 *
	 * @param name The name identifying what element we're getting
	 * @param associations Associations used to bind the generated element to it's parent
	 * @param contentTemplate The content wrapped by the element (if a container element)
	 * @param languages A list of languages you'd prefer, in order of most preferred to least preferred
	 *
	 * @return An instance of the named dynamic element. This can be a classless component (in which case it's the template name), a simple class name or a full class name
	 */
	public static NGElement dynamicElementWithName( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate, final List<String> languages ) {
		Objects.requireNonNull( name );
		Objects.requireNonNull( associations );
		Objects.requireNonNull( languages );

		// First we locate the class of the element we're going to render.
		final Class<? extends NGElement> elementClass = NGElementUtils.classWithNameNullIfNotFound( name );

		// If we don't find a class for the element, we're going to try going down the route of a classless component.
		if( elementClass == null ) {
			final NGComponentDefinition componentDefinition = _componentDefinition( name, languages );

			// FIXME: componentDefinition will probably never be null // Hugi 2022-10-10
			if( componentDefinition == null ) {
				throw new IllegalArgumentException( "Failed to construct a component definition for '%s'".formatted( name ) );
			}

			return componentDefinition.componentReferenceWithAssociations( associations, contentTemplate );
		}

		// First we check if this is a dynamic element
		if( NGDynamicElement.class.isAssignableFrom( elementClass ) ) {
			return NGElementUtils.createElement( elementClass, name, associations, contentTemplate );
		}

		// If it's not an element, let's move on to creating a component reference instead
		if( NGComponent.class.isAssignableFrom( elementClass ) ) {
			final NGComponentDefinition componentDefinition = _componentDefinition( (Class<? extends NGComponent>)elementClass, languages );

			// FIXME: componentDefinition will probably never be null // Hugi 2022-10-10
			if( componentDefinition == null ) {
				throw new IllegalArgumentException( "Failed to construct a component definition for '%s'".formatted( name ) );
			}

			return componentDefinition.componentReferenceWithAssociations( associations, contentTemplate );
		}

		// We should never end up here unless we got an incorrect/non-existent element name
		throw new IllegalArgumentException( "I could not construct a dynamic element named '%s'".formatted( name ) );
	}
}