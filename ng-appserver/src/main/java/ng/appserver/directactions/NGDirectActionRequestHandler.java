package ng.appserver.directactions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import ng.appserver.NGActionResults;
import ng.appserver.NGRequest;
import ng.appserver.NGRequestHandler;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGParsedURI;
import ng.appserver.templating.NGElementUtils;

/**
 * FIXME: Currently requires the full class name to be specified.
 * FIXME: We're going to want to cache both action classes and action methods
 */

public class NGDirectActionRequestHandler extends NGRequestHandler {

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
			// FIXME: We're using the Dynamic Element class locator to find the class by the simple name. This needs redesign // Hugi 2023-03-17
			final Class<? extends NGDirectAction> directActionClass = NGElementUtils.classWithNameNullIfNotFound( directActionClassName.get() );
			final Constructor<? extends NGDirectAction> constructor = directActionClass.getConstructor( NGRequest.class );
			final NGDirectAction instance = constructor.newInstance( request );
			final NGActionResults actionResults = instance.performActionNamed( directActionMethodName.get() );
			return actionResults.generateResponse();
		}
		catch( /* ClassNotFoundException |*/ InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			throw new RuntimeException( e );
		}
	}
}