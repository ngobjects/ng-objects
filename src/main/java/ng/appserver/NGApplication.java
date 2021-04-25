package ng.appserver;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGApplication {

	private static Logger logger = LoggerFactory.getLogger( NGApplication.class );

	private static NGApplication _application;

	/**
	 * FIXME: Needs to be thread safe?
	 */
	private Map<String, NGRequestHandler> _requestHandlers = new HashMap<>();

	/**
	 * FIXME: Not sure if this method should actually be provided 
	 */
	public static void main( final String[] args ) {
		main( args, NGApplication.class );
	}

	public static void main( final String[] args, final Class<? extends NGApplication> applicationClass ) {
		try {
			_application = applicationClass.getDeclaredConstructor().newInstance();
			_application.registerRequestHandler( "wo", new NGComponentRequestHandler() );
			_application.registerRequestHandler( "wr", new NGResourceRequestHandler() );
			_application.registerRequestHandler( "wa", new NGDirectActionRequestHandler() );
			_application.run();
		}
		catch( Exception e ) {
			e.printStackTrace();
			System.exit( -1 );
		}
	}

	private void run() {
		try {
			new NGJettyAdaptor().run();
		}
		catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public static NGApplication application() {
		return _application;
	}

	public void registerRequestHandler( final String key, final NGRequestHandler requestHandler ) {
		_requestHandlers.put( key, requestHandler );
	}

	public NGResponse dispatchRequest( final NGRequest request ) {
		final var uriWithoutPrecedingSlash = request.uri().substring(1);
		final String[] uriElements = uriWithoutPrecedingSlash.split( "/" );

		logger.info( "uri: " + uriWithoutPrecedingSlash.length() );
		logger.info( "uriElements: " + uriElements.length );

		// FIXME: Handle the case of no default request handler gracefully
		if( uriElements.length == 1 && uriWithoutPrecedingSlash.isEmpty() ) {
			return new NGResponse( "I have no idea to handle requests without an URL", 404 );
		}

		final var requestHandlerKey = uriElements[0];

		final NGRequestHandler requestHandler = _requestHandlers.get( requestHandlerKey );
		
		if( requestHandler == null ) {
			return new NGResponse( "No request handler found with key " + requestHandlerKey );
		}
		
		return requestHandler.handleRequest( request );
	}
}