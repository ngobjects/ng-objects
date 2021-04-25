package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class NGDirectAction {

	private final NGContext _context;

	public NGDirectAction( final NGRequest request ) {
		Objects.requireNonNull( request );
		_context = NGApplication.application().createContextForRequest( request );
	}

	public NGContext context() {
		return _context;
	}

	public NGRequest request() {
		return context().request();
	}

	public NGResponse response() {
		return context().response();
	}

	public NGComponent pageWithName( final Class<? extends NGComponent> componentClass ) {
		return NGApplication.application().pageWithName( null, context() );
	}

	/**
	 * Invokes the method with the given name + "Action" and returns the result. 
	 */
	public NGActionResults performActionNamed( final String directActionName ) {
		try {
			final Method method = getClass().getMethod( directActionName + "Action", new Class[] {} );
			return (NGActionResults)method.invoke( this, null );
		}
		// FIXME: All this error handling needs to be properly inspected.
		catch( NoSuchMethodException e ) {
			return new NGResponse( "No direct action method called " + directActionName, 404 );
		}
		catch( SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}
}