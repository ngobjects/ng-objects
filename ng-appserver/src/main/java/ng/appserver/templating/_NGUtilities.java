package ng.appserver.templating;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGRepetition;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;

public class _NGUtilities {

	/**
	 * @return A class matching classNameToSearch for. Searches by fully qualified class name and simpleName.
	 */
	public static Class classWithName( String classNameToSearchFor ) {
		Objects.requireNonNull( classNameToSearchFor );

		final List<Class> classes = List.of(
				NGString.class,
				NGImage.class,
				NGHyperlink.class,
				NGRepetition.class,
				NGStylesheet.class,
				TestComponent.class );

		for( Class c : classes ) {
			if( c.getName().equals( classNameToSearchFor ) || c.getSimpleName().equals( classNameToSearchFor ) ) {
				return c;
			}
		}

		throw new RuntimeException( "Class not found: " + classNameToSearchFor );
	}

	public static Class lookForClassInAllBundles( String s1 ) {
		throw new RuntimeException( "Not implemnted" );
	}

	/**
	 * Maps tag names to their dynamic element names
	 *
	 * FIXME: Definitely not the final home of this functionality // Hugi 2022-04-23
	 */
	public static Map<String, String> tagShortcutMap() {
		Map<String, String> m = new HashMap<>();
		m.put( "img", NGImage.class.getSimpleName() );
		m.put( "link", NGHyperlink.class.getSimpleName() );
		m.put( "repetition", NGRepetition.class.getSimpleName() );
		m.put( "str", NGString.class.getSimpleName() );
		m.put( "stylesheet", NGStylesheet.class.getSimpleName() );
		return m;
	}

	public static <E> E instantiateObject( Class<E> objectClass, Class[] parameterTypes, Object[] parameters ) {
		try {
			Constructor<E> constructor = objectClass.getDeclaredConstructor( parameterTypes );
			return constructor.newInstance( parameters );
		}
		catch( Throwable e ) {
			throw new RuntimeException( e );
		}
	}
}