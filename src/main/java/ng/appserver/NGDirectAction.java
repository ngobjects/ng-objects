package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class NGDirectAction {

	private NGRequest _request;

	public NGDirectAction( final NGRequest request ) {
		Objects.requireNonNull( request );
		_request = request;
	}

	public NGRequest request() {
		return _request;
	}

	/**
	 * Invokes the method with the given name + "Action" and returns the result. 
	 */
	public NGActionResults performActionNamed( final String directActionName ) {
		try {
			final Method method = getClass().getMethod( directActionName + "Action", new Class[] {} );
			return (NGActionResults)method.invoke( this, null );
		}
		catch( NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
			// FIXME: Handle this gracefully with a returned page.
			throw new RuntimeException( e );
		}
	}
}