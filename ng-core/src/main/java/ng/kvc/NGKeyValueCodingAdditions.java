package ng.kvc;

import java.lang.reflect.Field;
import java.util.Objects;

public interface NGKeyValueCodingAdditions extends NGKeyValueCoding {

	public Object valueForKeyPath( final String keyPath );

	public void takeValueForKeyPath( final Object value, final String keyPath );

	public static class Utility {

		public static Object valueForKeyPath( final Object object, final String keyPath ) {
			Objects.requireNonNull( object );
			Objects.requireNonNull( keyPath );

			if( object instanceof NGKeyValueCodingAdditions kvcAdditionsObject ) {
				return kvcAdditionsObject.valueForKeyPath( keyPath );
			}

			return DefaultImplementation.valueForKeyPath( object, keyPath );
		}

		/**
		 * FIXME: Very basic implementation for testing // Hugi 2022-04-23
		 */
		public static void takeValueForKeyPath( final Object object, final Object value, final String keyPath ) {
			Objects.requireNonNull( object );
			Objects.requireNonNull( keyPath );

			try {
				Field field = object.getClass().getField( keyPath );
				field.set( object, value );
			}
			catch( NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e ) {
				throw new RuntimeException( e );
			}
		}
	}

	public static class DefaultImplementation {

		/**
		 * Temporary implementation for testing
		 */
		public static Object valueForKeyPath( final Object object, final String keyPath ) {
			String[] keyPaths = keyPath.split( "\\." );

			Object result = object;

			for( String currentKeyPath : keyPaths ) {
				result = NGKeyValueCoding.Utility.valueForKey( result, currentKeyPath );

				if( result == null ) {
					return null;
				}
			}

			return result;
		}

		public void takeValueForKeyPath( Object object, Object target, String keyPath ) {
			throw new RuntimeException( "Not implemented" );
		}
	}
}