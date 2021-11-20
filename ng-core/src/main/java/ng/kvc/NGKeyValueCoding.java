package ng.kvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * NGKeyValueCoding is a simplified reimplementation of NSKeyValueCoding.
 *
 * FIXME: Currently only handles
 * FIXME: Implement missing key handling
 * FIXME: Decide if we're going to go all the way and do validation as well
 * FIXME: We should be marking bindings for directionality (read only/set only etc)
 * FIXME: We should be using cached MethodHandles for improved performance
 */

public interface NGKeyValueCoding {

	public Object valueForKey( String key );

	public void takeValueForKey( Object value, String key );

	public static class Utility {

		public static Object valueForKey( final Object object, final String key ) {
			Objects.requireNonNull( object );
			Objects.requireNonNull( key );

			if( object instanceof NGKeyValueCoding kvcObject ) {
				return kvcObject.valueForKey( key );
			}

			return DefaultImplementation.valueForKey( object, key );
		}
	}

	public static class DefaultImplementation {

		/**
		 * FIXME: Implement the correct method/field lookup ordering:
		 *
		 * 1. Method "getSmu"
		 * 2. Method "smu"
		 * 3. Method "isSmu"
		 * 4. Method "_getSmu"
		 * 5. Method "_smu"
		 * 6. Method "_isSmu"
		 * 7. Field "_smu"
		 * 8. Field "_isSmu"
		 * 9. Field "smu"
		 * 10. Field "isSmu"
		 *
		 * FIXME: Error handling
		 */
		public static Object valueForKey( final Object object, final String key ) {
			Objects.requireNonNull( object );
			Objects.requireNonNull( key );

			KVCBinding kvcBinding = bindingForKey( object.getClass(), key );
			// FIXME: Around here we should be handling the case of a missing key.
			return kvcBinding.valueInObject( object );
		}
	}

	/**
	 * @return A KVC binding for the given class and key.
	 */
	public static KVCBinding bindingForKey( Class<?> targetClass, String key ) {
		return new MethodBinding( targetClass, key );
	}

	public static interface KVCBinding {
		public Object valueInObject( Object object );
	}

	public static class MethodBinding implements KVCBinding {

		public final Method _method;

		public MethodBinding( Class<?> targetClass, String key ) {
			try {
				_method = targetClass.getMethod( key, new Class[] {} );
			}
			catch( NoSuchMethodException | SecurityException e ) {
				// FIXME: Error handling is missing entirely
				throw new RuntimeException( e );
			}
		}

		@Override
		public Object valueInObject( Object object ) {
			try {
				return _method.invoke( object, null );
			}
			catch( SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
				// FIXME: Error handling is missing entirely
				throw new RuntimeException( e );
			}
		}
	}

	public static class FieldBinding implements KVCBinding {

		@Override
		public Object valueInObject( Object object ) {
			throw new RuntimeException( "Not Implemented" );
		}
	}
}