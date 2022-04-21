package er.extensions.bettertemplates;

import java.lang.reflect.Constructor;

import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGString;

public class _NGUtilities {

	public static Class classWithName( String s1 ) {

		if( s1.contains( "NGString" ) ) {
			return NGString.class;
		}

		if( s1.contains( "NGImage" ) ) {
			return NGImage.class;
		}

		throw new RuntimeException( "Not implemented" );
	}

	public static Object instantiateObject( Class objectClass, Class[] parameterTypes, Object[] parameters, boolean shouldThrow, boolean shouldLog ) {
		try {
			if( (parameterTypes != null) && (parameters != null) ) {
				Constructor constructor = objectClass.getDeclaredConstructor( parameterTypes );
				return constructor.newInstance( parameters );
			}
			else {
				return objectClass.newInstance();
			}
		}
		catch( Throwable throwable ) {

			throwable.printStackTrace();
		}
		return null;
	}
}