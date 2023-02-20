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

	private final NGRequest _request;

	public NGDirectAction( final NGRequest request ) {
		Objects.requireNonNull( request );
		_request = request;
	}

	public NGContext context() {
		return _request.context();
	}

	public NGRequest request() {
		return _request;
	}

	/**
	 * The action invoked if no direct action name is specified
	 */
	public NGActionResults defaultAction() {
		return new NGResponse();
	}

	public <E extends NGComponent> E pageWithName( final Class<E> componentClass ) {
		return NGApplication.application().pageWithName( componentClass, context() );
	}

	/**
	 * Invokes the method with the given name + "Action" and returns the result.
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