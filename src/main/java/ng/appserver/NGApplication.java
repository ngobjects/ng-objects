package ng.appserver;

import java.util.HashMap;
import java.util.Map;

public class NGApplication {

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
			_application.registerRequestHandler( "wa", new NGDirectActionRequestHandler() );
			_application.run();
		}
		catch( Exception e ) {
			throw new RuntimeException( e );
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
		final String[] pathElements = request.uri().split( "/" );
		final NGRequestHandler requestHandler = _requestHandlers.get( pathElements[0] );
		return requestHandler.handleRequest( request );
	}
}