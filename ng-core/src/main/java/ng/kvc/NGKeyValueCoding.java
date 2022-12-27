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
 * FIXME: Implement the correct method/field lookup ordering:
 * FIXME: We're going to want to decide what to do if there's an available key, but not accessible. Do we skip to the next "type" of binding or do we throw. Essentially; is shading allowed.
 *
 * Lookup order when searching for a readable binding:
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
 * Lookup order when searching for a writable binding: // FIXME: Incomplete // Hugi 2022-12-27
 *
 * 1. Method "setSmu"
 * 2. Field "smu"
 * ...
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

		public static Object valueForKey( final Object object, final String key ) {
			Objects.requireNonNull( object );
			Objects.requireNonNull( key );

			final KVCReadBinding kvcBinding = readBindingForKey( object, key );

			if( kvcBinding == null ) {
				String message = String.format( "Unable to resolve key '%s' against class '%s'", key, object.getClass().getName() );
				throw new UnknownKeyException( message );
			}

			return kvcBinding.valueInObject( object );
		}

		public static void takeValueForKey( final Object object, final Object value, final String key ) {
			final KVCWriteBinding kvcBinding = writeBindingForKey( object, key );

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
	 * FIXME: KVC in WebObjects follows a slightly different method ordering. Our ordering is different in the way that we try for the exact key name first, for both methods and fields. In other terms, our lookup order is the same. Consider if this is the correct approach // Hugi 2022-10-22
	 */
	private static KVCReadBinding readBindingForKey( final Object object, final String key ) {
		Objects.requireNonNull( object );
		Objects.requireNonNull( key );

		Method method;

		// First we try for just the key
		method = readMethod( object, key );

		if( method != null ) {
			return new MethodReadBinding( method );
		}

		final String keyCapitalized = key.substring( 0, 1 ).toUpperCase() + key.substring( 1 );

		// Now we try the old bean-style getMethod()
		method = readMethod( object, "get" + keyCapitalized );

		if( method != null ) {
			return new MethodReadBinding( method );
		}

		// Then we go for the bean-style isMethod() for booleans
		method = readMethod( object, "is" + keyCapitalized );

		if( method != null ) {
			return new MethodReadBinding( method );
		}

		// _getMethod() (get-prefixed, prefixed with an underscore)
		method = readMethod( object, "_get" + keyCapitalized );

		if( method != null ) {
			return new MethodReadBinding( method );
		}

		// _method() (prefixed with an underscore)
		method = readMethod( object, "_" + key );

		if( method != null ) {
			return new MethodReadBinding( method );
		}

		// _isMethod() (is-prefixed, prefixed with an underscore)
		method = readMethod( object, "_is" + key );

		if( method != null ) {
			return new MethodReadBinding( method );
		}

		Field field;

		// First we try for just the key ("key")
		field = field( object, key );

		if( field != null ) {
			return new FieldBinding( field );
		}

		// Then check for the field with an underscore in front ("_key")
		field = field( object, "_" + key );

		if( field != null ) {
			return new FieldBinding( field );
		}

		// Then check for the field prefixed with "is" and an underscore ("_isKey") // FIXME: Why check for this first, rather than just "isKey" without the underscore? // Hugi 2022-10-22
		field = field( object, "_is" + keyCapitalized );

		if( field != null ) {
			return new FieldBinding( field );
		}

		// Finally check for the field prefixed with "is" ("isKey")
		field = field( object, "is" + keyCapitalized );

		if( field != null ) {
			return new FieldBinding( field );
		}

		return null;
	}

	/**
	 * FIXME: The list of methods/field names to lookup is not complete // Hugi 2022-12-27
	 */
	private static KVCWriteBinding writeBindingForKey( final Object object, final String key ) {
		Method method;

		final String keyCapitalized = key.substring( 0, 1 ).toUpperCase() + key.substring( 1 );

		// Now we try the old bean-style getMethod()
		method = writeMethod( object, "set" + keyCapitalized );

		if( method != null ) {
			return new MethodWriteBinding( method );
		}

		Field field;

		// First we try for just the key ("key")
		field = field( object, key );

		if( field != null ) {
			return new FieldBinding( field );
		}

		return null;
	}

	/**
	 * FIXME: Only look for methods that "return" Void // Hugi 2022-12-27
	 * FIXME: Getting the write method is going to require some much more complicated semantics, similar to what we're doing for readMethod // Hugi 2022-12-27
	 * FIXME: We might want to look at the class of the value we're setting, so we can support overloading. Not sure we even want to do that though... // Hugi 2022-12-27
	 */
	private static Method writeMethod( final Object object, final String key ) {
		for( Method method : object.getClass().getMethods() ) {
			if( method.getName().equals( key ) ) {
				if( method.getParameterCount() == 1 ) {
					return method;
				}
			}
		}

		return null;
	}

	/**
	 * @return The named method, accepting no parameters
	 *
	 * // FIXME: Are we checking the method's return type? I.e. does it actually return something? If not, do so // Hugi 2022-12-27
	 */
	private static Method readMethod( final Object object, final String key ) {
		return method( object, key );
	}

	/**
	 * @return The (exactly) named method if the class responds to it, null if not.
	 */
	private static Method method( final Object object, final String key, Class<?>... signature ) {
		Objects.requireNonNull( object );
		Objects.requireNonNull( key );

		Class<?> currentClass = object.getClass();

		try {
			while( currentClass != null ) {
				final Method classMethod = currentClass.getMethod( key, signature );

				if( classMethod.canAccess( object ) ) {
					// Method exists and is accessible on the object's class
					// This is the happy path, where we'll immediately end up in 99% of cases
					return classMethod;
				}

				// Here come the dragons...

				// The class doesn't have an accessible method definition. What about the interfaces?
				// FIXME: We're missing a check on "parent interfaces", i.e. interfaces that these interfaces inherit from // Hugi 2022-10-21
				for( Class<?> interfaceClass : currentClass.getInterfaces() ) {
					try {
						final Method interfaceMethod = interfaceClass.getMethod( key, signature );

						if( interfaceMethod.canAccess( object ) ) {
							return interfaceMethod;
						}
					}
					catch( NoSuchMethodException interfaceException ) {
						// Failure to locate methods in interfaces are to be expected. If no interfaces contain the method, we've already failed anyway.
					}
				}

				// Now let's try the whole thing again for the superclass
				currentClass = currentClass.getSuperclass();
			}

			// The method exists, but no accessible implementation was found. Tough luck.
			return null;
		}
		catch( NoSuchMethodException methodException ) {
			// We'll end up here immediately if the method doesn't exist on the first try.
			// If the method doesn't exist on the original class we're dead whatever we do regardless of accessibility, so just return immediately
			return null;
		}
	}

	/**
	 * @return The (exactly) named field if the class responds to it, null if not.
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

	public static interface KVCReadBinding {
		public Object valueInObject( final Object object );

	}

	public static interface KVCWriteBinding {
		public void setValueInObject( final Object value, final Object object );
	}

	public static class MethodReadBinding implements KVCReadBinding {

		private final Method _method;

		public MethodReadBinding( Method method ) {
			_method = method;
		}

		@Override
		public Object valueInObject( final Object object ) {
			try {
				return _method.invoke( object );
			}
			catch( SecurityException | IllegalAccessException | IllegalArgumentException e ) {
				// FIXME: Error handling is missing entirely
				throw new RuntimeException( e );
			}
			catch( InvocationTargetException e ) {
				// If the InvocationTargetException wraps a RuntimeException, just rethrow it. We're not adding any valuable information at the moment.
				if( e.getTargetException() instanceof RuntimeException r ) {
					throw r;
				}

				// If it's not a RuntimeException, wrap and throw
				throw new RuntimeException( e );
			}
		}

	}

	public static class MethodWriteBinding implements KVCWriteBinding {

		private final Method _method;

		public MethodWriteBinding( Method method ) {
			_method = method;
		}

		@Override
		public void setValueInObject( Object value, Object object ) {
			try {
				_method.invoke( object, value );
			}
			catch( IllegalAccessException e ) {
				// FIXME: Error handling is missing entirely
				throw new RuntimeException( e );
			}
			catch( IllegalArgumentException e ) {
				// FIXME: Error handling is missing entirely
				throw new RuntimeException( e );
			}
			catch( InvocationTargetException e ) {
				// FIXME: Error handling is missing entirely
				throw new RuntimeException( e );
			}
		}

	}

	/**
	 * FIXME: We should really implement separate read/write bindings for Fields for more fine grained control // Hugi 2022-12-27
	 */
	public static class FieldBinding implements KVCWriteBinding, KVCReadBinding {

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