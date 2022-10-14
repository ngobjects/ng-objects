package ng.kvc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import ng.NGRuntimeException;

/**
 * NGKeyValueCoding is a simplified reimplementation of NSKeyValueCoding.
 *
 * FIXME: Implement missing key handling
 * FIXME: Decide if we're going to go all the way and do validation as well
 * FIXME: We should be marking bindings for directionality (read only/set only etc)
 * FIXME: We should be using cached MethodHandles for improved performance
 * FIXME: We might want to do the same gymnastics with fields as methods in case of private fields getting passed down to private inner classes
 * FIXME: Error handling
 * FIXME: Why check for the underscore fields before the fully matching field names? We're basically just doing that to keep compatibility with NSKVC // Hugi 2022-01-02
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

		public static void takeValueForKey( final Object object, final Object value, final String key ) {
			Objects.requireNonNull( object );
			Objects.requireNonNull( key );

			if( object instanceof NGKeyValueCoding kvcObject ) {
				kvcObject.takeValueForKey( value, key );
			}

			DefaultImplementation.takeValueForKey( object, value, key );
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
		 */
		public static Object valueForKey( final Object object, final String key ) {
			Objects.requireNonNull( object );
			Objects.requireNonNull( key );

			final KVCBinding kvcBinding = bindingForKey( object, key );

			if( kvcBinding == null ) {
				String message = String.format( "Unable to resolve key '%s' against class '%s'", key, object.getClass().getName() );
				throw new UnknownKeyException( message );
			}

			return kvcBinding.valueInObject( object );
		}

		public static void takeValueForKey( final Object object, final Object value, final String key ) {
			final KVCBinding kvcBinding = bindingForKey( object, key );

			if( kvcBinding == null ) {
				String message = String.format( "Unable to resolve key '%s' against class '%s'", key, object.getClass().getName() );
				throw new UnknownKeyException( message );
			}

			kvcBinding.setValueInObject( value, object );
		}
	}

	/**
	 * @return A KVC binding for the given class and key.
	 *
	 * FIXME: We're going to want to decide what to do if there's an available key, but not accessible. Do we skip to the next "type" of binding or do we throw. Essentially; is shading allowed.
	 */
	public static KVCBinding bindingForKey( final Object object, final String key ) {
		Objects.requireNonNull( object );
		Objects.requireNonNull( key );

		Method method = method( object, key );

		if( method != null ) {
			return new MethodBinding( method );
		}

		final String getPrefixedKey = "get" + key.substring( 0, 1 ).toUpperCase() + key.substring( 1 );

		method = method( object, getPrefixedKey );

		if( method != null ) {
			return new MethodBinding( method );
		}

		Field field = field( object, key );

		if( field != null ) {
			return new FieldBinding( field );
		}

		return null;
	}

	/**
	 * @return true if targetClass responds to the methodName()
	 *
	 * Returns null if the method is not found.
	 */
	private static Method method( final Object object, final String key ) {
		Objects.requireNonNull( object );
		Objects.requireNonNull( key );

		Class<?> classToUseForLocatingMethod = object.getClass();

		try {
			while( classToUseForLocatingMethod != null ) {
				Method classMethod = classToUseForLocatingMethod.getMethod( key );

				if( classMethod.canAccess( object ) ) {
					// This is the happy path, where we'll immediately end up in 99% of cases
					return classMethod;
				}

				// Here come the dragons...

				// The class doesn't have an accessible method definition. What about the interfaces?
				for( Class<?> interfaceClass : classToUseForLocatingMethod.getInterfaces() ) {
					try {
						final Method interfaceMethod = interfaceClass.getMethod( key );

						if( interfaceMethod.canAccess( object ) ) {
							return interfaceMethod;
						}
					}
					catch( Exception interfaceException ) {
						// Failure to locate methods in interfaces are to be expected. If no interfaces contain the method, we've already failed anyway.
					}
				}

				// Now let's try the whole thing again for the superclass
				classToUseForLocatingMethod = classToUseForLocatingMethod.getSuperclass();
			}
		}
		catch( NoSuchMethodException | SecurityException methodException ) {
			// If the method doesn't exist on the original class we're dead whatever we do regardless of accessibility, so just throw immediately
			return null;
		}

		// FIXME: Don't throw
		return null;
	}

	/**
	 * @return true if targetClass has a field named [fieldName]
	 */
	private static Field field( final Object object, final String fieldName ) {
		Objects.requireNonNull( object );
		Objects.requireNonNull( fieldName );

		try {
			return object.getClass().getField( fieldName );
		}
		catch( NoSuchFieldException | SecurityException e ) {
			return null;
		}
	}

	public static interface KVCBinding {
		public Object valueInObject( final Object object );

		public void setValueInObject( final Object value, final Object object );
	}

	public static class MethodBinding implements KVCBinding {

		private final Method _method;

		public MethodBinding( Method method ) {
			_method = method;

		}

		@Override
		public Object valueInObject( final Object object ) {
			try {
				return _method.invoke( object );
			}
			catch( SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
				// FIXME: Error handling is missing entirely
				throw new RuntimeException( e );
			}
		}

		@Override
		public void setValueInObject( Object value, Object object ) {
			throw new RuntimeException( "Not implemented" );
		}
	}

	public static class FieldBinding implements KVCBinding {

		private final Field _field;

		public FieldBinding( Field field ) {
			_field = field;
		}

		@Override
		public Object valueInObject( Object object ) {
			try {
				return _field.get( object );
			}
			catch( SecurityException | IllegalAccessException | IllegalArgumentException e ) {
				// FIXME: Error handling is missing entirely
				throw new RuntimeException( e );
			}
		}

		@Override
		public void setValueInObject( Object value, Object object ) {
			try {
				_field.set( object, value );
			}
			catch( IllegalArgumentException | IllegalAccessException e ) {
				// FIXME: Error handling is missing entirely
				throw new RuntimeException( e );
			}
		}
	}

	/**
	 * Thrown when a key can't be resolved against an object
	 */
	public static class UnknownKeyException extends NGRuntimeException {

		public UnknownKeyException( String message ) {
			super( message );
		}
	}
}