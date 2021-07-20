package ng.kvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * FIXME: This isn't even remoitely implemented
 */

public class NGKeyValueCoding {

	public static class Utility {
		public static Object valueForKeyPath( final Object object, final String keyPath ) {
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

	public static void main( String[] args ) {
		final String s = "Hugi";
		System.out.println( "Length: " + Utility.valueForKeyPath( s, "length" ) );
	}
}