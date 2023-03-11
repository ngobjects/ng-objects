package ng.kvc;

import java.util.Objects;

/**
 * FIXME: We're missing unit tests for this // Hugi 2023-03-11
 */

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

		public static void takeValueForKeyPath( final Object object, final Object value, final String keyPath ) {
			Objects.requireNonNull( object );
			Objects.requireNonNull( keyPath );

			if( object instanceof NGKeyValueCodingAdditions kvcAdditionsObject ) {
				kvcAdditionsObject.takeValueForKeyPath( value, keyPath );
			}
			else {
				DefaultImplementation.takeValueForKeyPath( object, value, keyPath );
			}
		}
	}

	public static class DefaultImplementation {

		public static Object valueForKeyPath( final Object object, final String keyPath ) {
			final String[] keyPathComponents = keyPath.split( "\\." );

			Object result = object;

			for( String currentKeyPathComponent : keyPathComponents ) {
				result = NGKeyValueCoding.Utility.valueForKey( result, currentKeyPathComponent );

				if( result == null ) {
					return null;
				}
			}

			return result;
		}

		public static void takeValueForKeyPath( Object object, Object value, String keyPath ) {
			int lastPeriodIndex = keyPath.lastIndexOf( '.' );

			// No periods means it's just a single key, so we don't need to resolve the keyPath
			if( lastPeriodIndex == -1 ) {
				NGKeyValueCoding.Utility.takeValueForKey( object, value, keyPath );
			}
			else {
				final String targetPath = keyPath.substring( 0, lastPeriodIndex ); // The target object is the on represented by the keypath, up to the last element
				final String valueKey = keyPath.substring( lastPeriodIndex + 1 ); // Last element in the keyPath is the field we want to set on the object represented by targetPath
				final Object actualTargetObject = valueForKeyPath( object, targetPath );
				NGKeyValueCoding.Utility.takeValueForKey( actualTargetObject, value, valueKey );
			}
		}
	}
}