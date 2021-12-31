package ng.appserver.directactions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGDirectAction {

	private final NGContext _context;

	public NGDirectAction( final NGRequest request ) {
		Objects.requireNonNull( request );

		// FIXME: I'm not entirely sure we should be generating a new context here // Hugi 2021-12-31
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
		return NGApplication.application().pageWithName( componentClass, context() );
	}

	/**
	 * Invokes the method with the given name + "Action" and returns the result.
	 *
	 * FIXME: Error handling needs to be properly inspected here // Hugi 2021-12-31
	 */
	public NGActionResults performActionNamed( final String directActionName ) {
		try {
			final Method method = getClass().getMethod( directActionName + "Action", new Class[] {} );
			return (NGActionResults)method.invoke( this );
		}
		catch( final NoSuchMethodException e ) {
			return new NGResponse( "No direct action method called " + directActionName, 404 );
		}
		catch( SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}
}