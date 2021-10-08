package ng.kvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public interface NGKeyValueCoding {

	public Object valueForKey( String key );
	
	public void takeValueForKey( Object value, String key );

	public static class Utility {
		public static Object valueForKey( final Object object, final String keyPath ) {
			Objects.requireNonNull( keyPath );
			try {
				final Method method = object.getClass().getMethod( keyPath, new Class[] {} );
				return method.invoke( object, null );
			}
			catch( NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
				throw new RuntimeException( e );
			}
		}
	}
}