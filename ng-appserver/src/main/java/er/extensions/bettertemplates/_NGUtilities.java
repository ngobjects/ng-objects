package er.extensions.bettertemplates;

import java.lang.reflect.Constructor;
import java.util.List;

import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;

public class _NGUtilities {

	private static List<Class> classes() {
		return List.of( NGString.class, NGImage.class, NGHyperlink.class, NGStylesheet.class );
	}

	public static Class classWithName( String className ) {

		for( Class c : classes() ) {
			if( c.getName().contains( className ) ) {
				return c;
			}
		}

		throw new RuntimeException( "Clas not found: " + className );
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