package ng.appserver.directactions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ng.appserver.NGActionResults;
import ng.appserver.NGRequest;
import ng.appserver.NGRequestHandler;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGParsedURI;

/**
 * CHECKME: We need to cache both action classes and methods // Hugi 2024-08-14
 */

public class NGDirectActionRequestHandler extends NGRequestHandler {

	private static Map<String, Class<? extends NGDirectAction>> _directActionClasses = new HashMap<>();

	/**
	 * The default path prefix for this request handler
	 */
	public static final String DEFAULT_PATH = "/wa/";

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		final NGParsedURI parsedURI = NGParsedURI.of( request.uri() );

		final Optional<String> directActionClassName = parsedURI.getStringOptional( 1 );

		if( directActionClassName.isEmpty() ) {
			return new NGResponse( "No direct action class name specified", 404 );
		}

		Optional<String> directActionMethodName = parsedURI.getStringOptional( 2 );

		if( directActionMethodName.isEmpty() ) {
			directActionMethodName = Optional.of( "default" );
		}

		try {
			final Class<? extends NGDirectAction> directActionClass = _directActionClasses.get( directActionClassName.get() );
			final Constructor<? extends NGDirectAction> constructor = directActionClass.getConstructor( NGRequest.class );
			final NGDirectAction instance = constructor.newInstance( request );
			final NGActionResults actionResults = instance.performActionNamed( directActionMethodName.get() );
			return actionResults.generateResponse();
		}
		catch( /* ClassNotFoundException |*/ InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * FIXME: This is a horrid method of registering Zdirect action classes // Hugi 2025-04-18
	 */
	public static void registerDirectActionClass( Class<? extends NGDirectAction> directActionClass ) {
		_directActionClasses.put( directActionClass.getName(), directActionClass );
		_directActionClasses.put( directActionClass.getSimpleName(), directActionClass );
	}
}