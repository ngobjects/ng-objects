package ng.appserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGProperties.PropertiesSourceArguments;
import ng.appserver.NGProperties.PropertiesSourceResource;
import ng.appserver.directactions.NGDirectActionRequestHandler;
import ng.appserver.resources.NGResource;
import ng.appserver.resources.NGResourceLoader;
import ng.appserver.resources.NGResourceLoader.JavaClasspathResourceSource;
import ng.appserver.resources.NGResourceManager;
import ng.appserver.resources.NGResourceManagerDynamic;
import ng.appserver.resources.StandardNamespace;
import ng.appserver.resources.StandardResourceType;
import ng.appserver.routing.NGRouteTable;
import ng.appserver.templating.NGElementUtils;
import ng.appserver.wointegration.NGDefaultLifeBeatThread;
import ng.appserver.wointegration.WOMPRequestHandler;
import ng.classes.NGClassManager;
import ng.plugins.NGPlugin;
import x.junk.NGExceptionPage;
import x.junk.NGExceptionPageDevelopment;
import x.junk.NGSessionTimeoutPage;
import x.junk.NGWelcomePage;

/**
 * FIXME: Initialization still feels a little weird, while we're moving away from the way it's handled in WOApplication. Look a little more into the flow of application initialization // Hugi 2021-12-29
 */

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
	 * Handles dynamic locating of classes used
	 */
	private NGClassManager _classManager;

	/**
	 * Resource loading, caching and management
	 */
	private NGResourceManager _resourceManager;

	/**
	 * Dynamic resource loading, caching and management
	 */
	private NGResourceManagerDynamic _resourceManagerDynamic;

	/**
	 * A list of patterns that will be applied to URLs before they are processed by the framework
	 */
	private List<Pattern> _urlRewritePatterns;

	/**
	 * In the old WO world, this would have been called "requestHandlers".
	 * Since we want to have more dynamic route resolution, it makes sense to move that to a separate object.
	 */
	private List<NGRouteTable> _routeTables = new ArrayList<>();

	/**
	 * Run the application
	 */
	public static void run( final String[] args, final Class<? extends NGApplication> applicationClass ) {
		runAndReturn( args, applicationClass );
	}

	/**
	 * Run the application and return the NGApplication instance
	 */
	public static <E extends NGApplication> E runAndReturn( final String[] args, final Class<E> applicationClass ) {
		final long startTime = System.currentTimeMillis();

		final NGProperties properties = new NGProperties();
		properties.addAndReadSourceHighestPriority( new PropertiesSourceArguments( args ) );

		// We need to start out with initializing logging to ensure we're seeing everything the application does during the init phase.
		redirectOutputToFilesIfOutputPathSet( properties.propWOOutputPath() );

		// FIXME: Since we've currently got no application instance, we can't yet query the application about mode (which is a property on the applicaiton instance). Lame and needs a fix // Hugi 2024-06-14
		final boolean isDevelopmentModeBotchedVersion = !properties.propWOMonitorEnabled(); /* properties.isDevelopmentMode() */

		if( isDevelopmentModeBotchedVersion ) {
			logger.info( "========================================" );
			logger.info( "===== Running in development mode! =====" );
			logger.info( "========================================" );
		}
		else {
			logger.info( "=======================================" );
			logger.info( "===== Running in production mode! =====" );
			logger.info( "=======================================" );
		}

		logger.info( "===== Properties from arguments =====\n" + properties._propertiesMapAsString() );

		try {
			NGApplication application = applicationClass.getDeclaredConstructor().newInstance();

			addDefaultResourceSources( application.resourceManager() );

			// FIXME: Assigning that unwanted global application...
			_application = application;

			// FIXME: Properties should be accessible during application initialization, probably passed to NGApplication's constructor
			application._properties = properties;

			// FIXME: We're adding the properties file here since it will use and needs application().resourceManager() to function at the moment // Hugi 2024-06-14
			properties.addAndReadSourceLowestPriority( new PropertiesSourceResource( StandardNamespace.App.identifier(), "Properties" ) );

			logger.info( "===== Properties after loading application properties =====\n" + properties._propertiesMapAsString() );

			// What we're doing here is allowing for the WO URL structure, which is required for us to work with the WO Apache Adaptor.
			// Ideally, we don't want to prefix URLs at all, instead just handling requests at root level.
			application._urlRewritePatterns.add( Pattern.compile( "^/(cgi-bin|Apps)/WebObjects/" + properties.propWOApplicationName() + ".woa(/[0-9])?" ) );

			// FIXME: This is probably not the place to load plugins. Probably need more extension points for plugin initialization (pre-constructor, post-constructor etc.) // Hugi 2023-07-28
			// We should also allow users to manually register plugins they're going to use for each NGApplication instance, as an alternative to greedily autoloading every provided plugin on the classpath
			application.loadPlugins();

			// The application class' package gets added by default // FIXME: Don't like this Hugi 2022-10-10
			NGElementUtils.addPackage( applicationClass.getPackageName() );

			// FIXME: Eventually the adaptor startup should probably be done by the user
			application.createAdaptor().start( application );

			if( properties.propWOLifebeatEnabled() ) {
				NGDefaultLifeBeatThread.start( application._properties );
			}

			logger.info( "===== Application started in {} ms at {}", (System.currentTimeMillis() - startTime), LocalDateTime.now() );

			return (E)application;
		}
		catch( final Exception e ) {
			// CHECKME: We're going to want to check for/identify certain error conditions an respond to them with an explanation, rather than silently exploding // Hugi 2022-10-22
			e.printStackTrace();
			System.exit( -1 );

			// Dead return to satisfy the java compiler (which isn't aware that it's just been violently stabbed to death using System.exit())
			return null;
		}
	}

	/**
	 * Construct an application with no properties (and no property loading)
	 */
	public NGApplication() {
		_classManager = new NGClassManager();
		_resourceManager = new NGResourceManager();
		_resourceManagerDynamic = new NGResourceManagerDynamic();
		_sessionStore = new NGServerSessionStore();
		_urlRewritePatterns = new ArrayList<>();

		// The first table in the list is the "user route table"
		_routeTables.add( new NGRouteTable( "User routes" ) );

		_routeTables.add( createSystemRoutes() );
	}

	/**
	 * @return A table containing our "built-in routes"
	 */
	private NGRouteTable createSystemRoutes() {
		// Then we add the "system route table"
		final NGRouteTable systemRoutes = new NGRouteTable( "System routes" );
		systemRoutes.map( "/", this::defaultResponse );
		systemRoutes.map( NGComponentRequestHandler.DEFAULT_PATH + "*", new NGComponentRequestHandler() );
		systemRoutes.map( NGResourceRequestHandler.DEFAULT_PATH + "*", new NGResourceRequestHandler() );
		systemRoutes.map( NGResourceRequestHandlerDynamic.DEFAULT_PATH + "*", new NGResourceRequestHandlerDynamic() );
		systemRoutes.map( NGDirectActionRequestHandler.DEFAULT_PATH + "*", new NGDirectActionRequestHandler() );
		systemRoutes.map( WOMPRequestHandler.DEFAULT_PATH, new WOMPRequestHandler() );
		systemRoutes.map( "/sessionCookieReset", this::resetSessionCookie );
		return systemRoutes;
	}

	/**
	 * FIXME: This needs a better mechanism overall // Hugi 2024-03-17
	 */
	private NGActionResults resetSessionCookie() {
		return resetSessionCookieWithRedirectToURL( "/" );
	}

	/**
	 * FIXME: This method should not exist, it's currently used by subclasses as a workaround for some bad session management // Hugi 2024-06-29
	 */
	protected NGActionResults resetSessionCookieWithRedirectToURL( final String url ) {
		final NGResponse response = new NGResponse();

		response.setHeader( "location", url );
		response.setStatus( 302 );
		response.setHeader( "content-type", "text/html" );
		response.setHeader( "content-length", "0" );
		response.addCookie( createSessionCookie( "SessionCookieKillerCookieValuesDoesNotMatter", 0 ) );

		return response;
	}

	/**
	 * Locates plugins and loads them.
	 */
	private void loadPlugins() {
		ServiceLoader.load( NGPlugin.class )
				.stream()
				.map( Provider::get )
				.forEach( plugin -> {
					logger.info( "Loading plugin {}", plugin.getClass().getName() );
					plugin.load( this );
				} );
	}

	/**
	 * @return The class to use when constructing a new session. By default we look for a class named "Session" in the same package as the application class
	 */
	@SuppressWarnings("unchecked")
	protected Class<? extends NGSession> _sessionClass() {
		final String sessionClassName = getClass().getPackageName() + ".Session";
		Class<? extends NGSession> sessionClass;

		try {
			sessionClass = (Class<? extends NGSession>)Class.forName( sessionClassName );
		}
		catch( ClassNotFoundException e ) {
			logger.info( "Custom session class '{}' not found. Defaulting to '{}'", sessionClassName, NGSession.class.getName() );
			sessionClass = NGSession.class;
		}

		return sessionClass;
	}

	/**
	 * @return The fully qualified class name of the http adaptor
	 *
	 * CHECKME: This should eventually return the name of our own adaptor. Using Jetty for now (since it's easier to implement) // Hugi 2021-12-29
	 */
	public String adaptorClassName() {
		return "ng.adaptor.jetty.NGAdaptorJetty";
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

			// Dead return to satisfy the java compiler (which isn't aware that it's just been violently stabbed to death using System.exit())
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
	 * FIXME: In any case, this is going to have to be cached in some way, since it gets invoked _a lot_ // Hugi 2024-03-23
	 */
	public boolean isDevelopmentMode() {
		return _properties.isDevelopmentMode();
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

	/**
	 * @return The global NGApplication instance.
	 */
	@Deprecated
	public static NGApplication application() {
		return _application;
	}

	public NGResourceManager resourceManager() {
		return _resourceManager;
	}

	public NGResourceManagerDynamic resourceManagerDynamic() {
		return _resourceManagerDynamic;
	}

	public NGSessionStore sessionStore() {
		return _sessionStore;
	}

	public NGResponse dispatchRequest( final NGRequest request ) {

		try {
			rewriteURL( request );

			final NGRequestHandler requestHandler = handlerForURL( request.uri() );

			if( requestHandler == null ) {
				return noHandlerResponse( request );
			}

			final NGResponse response = requestHandler.handleRequest( request );

			if( response == null ) {
				throw new NullPointerException( String.format( "'%s' returned a null response. That's just rude.", requestHandler.getClass().getName() ) );
			}

			// FIXME: Doesn't feel like the place to set the session ID in the response, but let's do it anyway :D // Hugi 2023-01-10
			addSessionCookieToResponse( request, response );

			// FIXME: Same thought here. dispatchRequest() just doesn't feel like the place for session management // Hugi 2024-07-10
			touchSessionIfPresentAndNotTerminating( request );

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

	/**
	 * Add the sessionID cookie (if present in request) to the given response or, if the session is marked for termination, delete the session cookie
	 *
	 * FIXME: This might be a prime location to perform a session cookie deletion if (a) no sessionID is present or (b) no session is available for the given sessionID // Hugi 2024-07-10
	 */
	private void addSessionCookieToResponse( final NGRequest request, final NGResponse response ) {
		final String sessionID = request._sessionID();

		if( sessionID != null ) {
			final NGSession session = request.existingSession();

			if( session != null ) { // FIXME: existingSession() isn't really a reliable way to get the session (at least not yet)  // Hugi 2023-01-11
				if( session.shouldTerminate() ) {
					// If the session is terminating, delete the client side session cookie
					response.addCookie( createSessionCookie( "SessionCookieKillerCookieValuesDoesNotMatter", 0 ) );
					// CHECKME: This might be a better location to ask session storage to dispose of a terminated session.
				}
				else {
					response.addCookie( createSessionCookie( sessionID, (int)session.timeOut().toSeconds() ) );
				}
			}
		}
	}

	/**
	 * "touch" the requests's session, i.e. indicate we're still working with it, granting it life for an additional [sessionTimeout] seconds
	 */
	private void touchSessionIfPresentAndNotTerminating( final NGRequest request ) {
		final NGSession session = request.existingSession();

		if( session != null && !session.shouldTerminate() ) {
			session.touch();
		}
	}

	/**
	 * Invoked to generate a response if no requestHandler was found for the given request. Essentially a 404 response.
	 *
	 * CHECKME: This currently incorporates a very experimental "public resources" handler, essentially a plain web server. This is not final // Hugi 2023-07-20
	 */
	private NGResponse noHandlerResponse( final NGRequest request ) {

		// FIXME: We've not decided what to do about namespacing and public resources // Hugi 2024-06-26
		final String namespace = "app";
		final String resourcePath = request.uri();

		if( resourcePath.isEmpty() ) {
			return new NGResponse( "No resource name specified", 400 );
		}

		final Optional<NGResource> resource = resourceManager().obtainPublicResource( namespace, resourcePath );

		// FIXME: Shouldn't we allow the user to customize the response for a non-existent resource? // Hugi 2024-02-05
		if( resource.isEmpty() ) {
			final NGResponse errorResponse = new NGResponse( "public resource '" + resourcePath + "' does not exist", 404 );
			errorResponse.setHeader( "content-type", "text/html" );
			return errorResponse;
		}

		return NGResourceRequestHandler.responseForResource( resource.get(), resourcePath );
	}

	/**
	 * @return A session cookie
	 */
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
		return pageWithName( NGWelcomePage.class, request.context() );
	}

	/**
	 * Invoked by dispatchRequest before the request is handled to apply all url-rewriting patterns.
	 *
	 * CHECKME: Not really a fan of including this functionality, but it helps out with WO adaptor compatibility.
	 */
	private void rewriteURL( final NGRequest request ) {

		for( Pattern pattern : _urlRewritePatterns ) {
			final Matcher matcher = pattern.matcher( request.uri() );

			if( matcher.find() ) {
				logger.debug( "Rewriting: {}", request.uri() );
				request.setURI( request.uri().substring( matcher.group().length() ) );
				logger.debug( "Rewrote: {}", request.uri() );
			}
		}

		// FIXME: Currently required to handle a little fun with how WO adaptor URLs are structured and how we rewrite them. Investigate if we can fix this at a more sane level // Hugi 2024-06-17
		if( request.uri().isEmpty() ) {
			request.setURI( "/" );
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
	 * FIXME: This needs cleanup // Hugi 2024-08-14
	 */
	private static void addDefaultResourceSources( final NGResourceManager resourceManager ) {
		final NGResourceLoader loader = resourceManager.resourceLoader();

		// FIXME: These are the "unnamespaced" resource locations we started out with. They'll still work fine, but we'll need to consider their future // Hugi 2024-06-19
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.App, new JavaClasspathResourceSource( "app-resources" ) );
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.WebServer, new JavaClasspathResourceSource( "webserver-resources" ) );
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.Public, new JavaClasspathResourceSource( "public" ) );
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.ComponentTemplate, new JavaClasspathResourceSource( "components" ) );

		// "app" namespace defined
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.App, new JavaClasspathResourceSource( "ng/app/app-resources" ) );
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.WebServer, new JavaClasspathResourceSource( "ng/app/webserver-resources" ) );
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.Public, new JavaClasspathResourceSource( "ng/app/public" ) );
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.ComponentTemplate, new JavaClasspathResourceSource( "ng/app/components" ) );
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
	@SuppressWarnings("unchecked") // Our cast to the component class is fine
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
	 * @return The componentDefinition corresponding to the given NGComponent class.
	 *
	 * FIXME: Languages are currently not supported. Parameter still included while we ponder localization // Hugi 2023-04-14
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
	 * FIXME: Languages are currently not supported. Parameter still included while we ponder localization // Hugi 2023-04-14
	 * FIXME: This should not be static // Hugi 2023-04-14
	 */
	public static NGComponentDefinition _componentDefinition( final String componentName, final List<String> languages ) {
		Objects.requireNonNull( componentName );
		Objects.requireNonNull( languages );

		return NGComponentDefinition.get( componentName );
	}

	/**
	 * FIXME: Languages are currently not supported. Parameter still included while we ponder localization // Hugi 2023-04-14
	 * FIXME: This should not be static // Hugi 2023-04-14
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
			return createDynamicElementInstance( elementClass, name, associations, contentTemplate );
		}

		// If it's not an element, let's move on to creating a component reference instead
		if( NGComponent.class.isAssignableFrom( elementClass ) ) {
			final NGComponentDefinition componentDefinition = _componentDefinition( (Class<? extends NGComponent>)elementClass, languages );
			return componentDefinition.componentReferenceWithAssociations( associations, contentTemplate );
		}

		// We should never end up here unless we got an incorrect/non-existent element name
		throw new IllegalArgumentException( "I could not construct a dynamic element named '%s'".formatted( name ) );
	}

	/**
	 * @return A new NGDynamicElement constructed using the given parameters
	 *
	 * CHECKME: Not sure this is the final home for this functionality. It's just a method shortcut to invoking the dynamic element's constructor via reflection.
	 */
	private static <E extends NGElement> E createDynamicElementInstance( final Class<E> elementClass, final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		final Class<?>[] parameterTypes = { String.class, Map.class, NGElement.class };
		final Object[] parameters = { name, associations, contentTemplate };

		try {
			final Constructor<E> constructor = elementClass.getDeclaredConstructor( parameterTypes );
			return constructor.newInstance( parameters );
		}
		catch( NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}
}