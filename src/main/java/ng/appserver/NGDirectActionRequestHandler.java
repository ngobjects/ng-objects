package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import ng.appserver.privates.NGParsedURI;

/**
 * FIXME: Currently requires the full class name to be specified. 
 */

public class NGDirectActionRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		NGParsedURI parsedURI = NGParsedURI.of( request.uri() );
		
		final Optional<String> directActionClassName = parsedURI.elementAt( 1 );
		
		if( directActionClassName.isEmpty() ) {
			return new NGResponse( "No direct action class name specified", 404 );
		}
		
		final Optional<String> directActionMethodName = parsedURI.elementAt( 2 );

		if( directActionMethodName.isEmpty() ) {
			return new NGResponse( "No direct action class name specified", 404 );
		}

		// FIXME: Improve error handling
		try {
			Class<? extends NGDirectAction> directActionClass = (Class<? extends NGDirectAction>)Class.forName( directActionClassName.get() );
			return directActionClass.getConstructor().newInstance().performActionNamed( directActionMethodName.get() ).generateResponse();
		}
		catch( ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			e.printStackTrace();
			return new NGResponse( "Error, error!", 500 );
		}
		
	}
}