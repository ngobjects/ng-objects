package ng.appserver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import ng.appserver.privates.NGParsedURI;

/**
 * FIXME: Currently requires the full class name to be specified.
 */

public class NGDirectActionRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		final NGParsedURI parsedURI = NGParsedURI.of( request.uri() );

		final Optional<String> directActionClassName = parsedURI.elementAt( 1 );

		if( directActionClassName.isEmpty() ) {
			return new NGResponse( "No direct action class name specified", 404 );
		}

		final Optional<String> directActionMethodName = parsedURI.elementAt( 2 );

		if( directActionMethodName.isEmpty() ) {
			return new NGResponse( "No direct action method name specified", 404 );
		}

		try {
			final Class<? extends NGDirectAction> directActionClass = (Class<? extends NGDirectAction>)Class.forName( directActionClassName.get() );
			final Constructor<? extends NGDirectAction> constructor = directActionClass.getConstructor( NGRequest.class );
			final NGDirectAction instance = constructor.newInstance( request );
			final NGActionResults actionResults = instance.performActionNamed( directActionMethodName.get() );
			return actionResults.generateResponse();
		}
		catch( ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			// FIXME: Improve error handling
			e.printStackTrace();
			return new NGResponse( "Error, error!", 500 );
		}
	}
}