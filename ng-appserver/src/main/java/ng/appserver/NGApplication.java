package ng.appserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.directactions.NGDirectActionRequestHandler;
import ng.appserver.privates.NGAdminAction;
import ng.appserver.properties.NGProperties;
import ng.appserver.properties.NGProperties.PropertiesSourceArguments;
import ng.appserver.properties.NGProperties.PropertiesSourceResource;
import ng.appserver.resources.NGResource;
import ng.appserver.resources.NGResourceLoader;
import ng.appserver.resources.NGResourceLoader.JavaClasspathResourceSource;
import ng.appserver.resources.NGResourceManager;
import ng.appserver.resources.NGResourceManagerDynamic;
import ng.appserver.resources.StandardNamespace;
import ng.appserver.resources.StandardResourceType;
import ng.appserver.routing.NGRouteTable;
import ng.appserver.templating.NGElementManager;
import ng.appserver.templating.NGElementUtils;
import ng.appserver.wointegration.NGDefaultLifeBeatThread;
import ng.appserver.wointegration.WOMPRequestHandler;
import ng.plugins.NGPlugin;
import ng.xperimental.NGExceptionPage;
import ng.xperimental.NGExceptionPageDevelopment;
import ng.xperimental.NGSessionTimeoutPage;
import ng.xperimental.NGWelcomePage;

/**
 * Where everything related to an application really meets up
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
	 * Handles templates and templating
	 */
	private NGElementManager _elementManager;

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
	 * Mode we're running in. Defaults to development mode unless explicitly set to a different mode through the application's startup parameters.
	 *
	 * FIXME: This requires modification to support more 'modes' (testing, staging etc.) // Hugi 2025-03-16
	 */
	private boolean _isDevelopmentMode = true;

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

		// Determine the mode we're currently running in. CHECKME: This should be configured using an explicit parameter // Hugi 2025-03-16
		final boolean isDevelopmentMode = !properties.propWOMonitorEnabled();

		if( isDevelopmentMode ) {
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

			// FIXME: Setting the application's mode. This should really be done in the constructor, since that's somewhere you'd _really_ like to be able to access the mode // Hugi 2025-03-16
			_application._isDevelopmentMode = isDevelopmentMode;

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
			application._elementManager.registerElementPackage( applicationClass.getPackageName() );

			// FIXME: Registering for the instance stopper to work. Horrid stuff. We need to convert NGAdminAction to routes // Hugi 2025-03-16
			application._elementManager.registerElementClass( NGAdminAction.class );

			// FIXME: Eventually the adaptor startup should probably be done by the user
			application.createAdaptor().start( application );

			// FIXME: For loading up our standard components. This should be moved to a separate module // Hugi 2025-03-16
			NGElementUtils.init();

			if( properties.propWOLifebeatEnabled() ) {
				NGDefaultLifeBeatThread.start( application._properties );
			}

			logger.info( "===== Application started in {} ms at {}", (System.currentTimeMillis() - startTime), LocalDateTime.now() );

			return (E)application;
		}
		catch( final Exception e ) {
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
		_elementManager = new NGElementManager();
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

	/**
	 * @return The Application's properties
	 */
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
	private NGRequestHandler handlerForURL( String url ) {
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
	 * @return true if we're in development mode
	 */
	public boolean isDevelopmentMode() {
		return _isDevelopmentMode;
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

	public NGElementManager elementManager() {
		return _elementManager;
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

			// FIXME: This is not the right place for session ID management // Hugi 2024-10-12
			addSessionCookieToResponse( request, response );
			touchSessionIfPresentAndNotTerminating( request );

			return response;
		}
		catch( final NGSessionRestorationException e ) {
			return responseForSessionRestorationException( e ).generateResponse();
		}
		catch( final NGPageRestorationException e ) {
			return responseForPageRestorationException( e ).generateResponse();
		}
		catch( final Throwable throwable ) {
			handleException( throwable );
			return responseForException( throwable, request.context() ).generateResponse();
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

			if( session != null ) {
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
		sessionCookie.setSameSite( "Lax" );
		// sessionCookie.setDomain( ... ) // FIXME: Implement // Hugi 2023-01-11
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
	protected NGActionResults responseForSessionRestorationException( final NGSessionRestorationException exception ) {
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
	protected NGActionResults responseForPageRestorationException( final NGPageRestorationException exception ) {
		return new NGResponse( exception.getMessage(), 404 );
	}

	/**
	 * @return The response generated when an exception occurs
	 */
	public NGActionResults responseForException( final Throwable throwable, final NGContext context ) {

		// FIXME: The originating context might have been an Ajax request, meaning the exception page won't render squat, which isn't helpful.
		// We should(a) not render the exception pace in the original context where the exception happened and/or (b) have a better,
		// more generic mechanism to ignore the context's updateContainers (in other words, we need a better way to control rendering from the server side)
		// Hugi 2024-10-09
		context.forceFullUpdate = true;

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
	 * CHECKME: All the "create..." methods here are really turning this into a factory. We might have to reconsider this design, not least if we decide to migrate to DI at some point // Hugi 2021-12-29
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

		// FIXME: These are the "unnamespaced" resource locations we started out with. They'll still work fine, but we'll need to consider their future and should probably be deleted // Hugi 2024-06-19
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.App, new JavaClasspathResourceSource( "app-resources" ) );
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.WebServer, new JavaClasspathResourceSource( "webserver-resources" ) );
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.Public, new JavaClasspathResourceSource( "public" ) );
		loader.addResourceSource( StandardNamespace.App.identifier(), StandardResourceType.ComponentTemplate, new JavaClasspathResourceSource( "components" ) );

		addDefaultResourcesourcesForNamespace( loader, StandardNamespace.App );
		addDefaultResourcesourcesForNamespace( loader, StandardNamespace.NG );
	}

	/**
	 * Declare resource sources for the standard resource types in the given namespace
	 */
	private static void addDefaultResourcesourcesForNamespace( final NGResourceLoader loader, final StandardNamespace namespace ) {
		final String id = namespace.identifier();

		loader.addResourceSource( id, StandardResourceType.App, new JavaClasspathResourceSource( "ng/%s/app-resources".formatted( id ) ) );
		loader.addResourceSource( id, StandardResourceType.WebServer, new JavaClasspathResourceSource( "ng/%s/webserver-resources".formatted( id ) ) );
		loader.addResourceSource( id, StandardResourceType.Public, new JavaClasspathResourceSource( "ng/%s/public".formatted( id ) ) );
		loader.addResourceSource( id, StandardResourceType.ComponentTemplate, new JavaClasspathResourceSource( "ng/%s/components".formatted( id ) ) );
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
	 *
	 * CHECKME: The implementation of this method is in elementManager, we're currently only keeping this method around for API compatibility with older projects.
	 */
	public NGComponent pageWithName( final String componentName, final NGContext context ) {
		return elementManager().pageWithName( componentName, context );
	}

	/**
	 * @return A new instance of [componentClass] in the given [context]
	 *
	 * CHECKME: The implementation of this method is in elementManager, we're currently only keeping this method around for API compatibility with older projects.
	 */
	public <E extends NGComponent> E pageWithName( final Class<E> componentClass, final NGContext context ) {
		return elementManager().pageWithName( componentClass, context );
	}
}