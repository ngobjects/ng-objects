package ng.kvc;

import java.util.Objects;

public interface NGKeyValueCodingAdditions extends NGKeyValueCoding {

	public Object valueForKeyPath( final String keyPath );

	public void takeValueForKeyPath( final Object value, final String keyPath );

	public static class Utility {

		public static Object valueForKeyPath( final Object object, final String key ) {
			Objects.requireNonNull( object );
			Objects.requireNonNull( key );

			if( object instanceof NGKeyValueCodingAdditions kvcAdditionsObject ) {
				return kvcAdditionsObject.valueForKeyPath( key );
			}

			return DefaultImplementation.valueForKeyPath( object, key );
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