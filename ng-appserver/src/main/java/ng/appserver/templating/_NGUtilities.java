package ng.appserver.templating;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGElement;
import ng.appserver.elements.NGComponentContent;
import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGRepetition;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;
import ng.appserver.elements.NGTextField;
import x.junk.TestComponent;

public class _NGUtilities {

	private static final Logger logger = LoggerFactory.getLogger( _NGUtilities.class );

	private static final List<Class<? extends NGElement>> _classes = new ArrayList<>();

	public static void addClass( final Class<? extends NGElement> clazz ) {
		_classes.add( clazz );
	}

	static {
		addClass( NGComponentContent.class );
		addClass( NGString.class );
		addClass( NGImage.class );
		addClass( NGHyperlink.class );
		addClass( NGRepetition.class );
		addClass( NGStylesheet.class );
		addClass( NGTextField.class );
		addClass( TestComponent.class );
	}

	/**
	 * @return A class matching classNameToSearch for. Searches by fully qualified class name and simpleName.
	 */
	public static Class classWithName( String classNameToSearchFor ) {
		Objects.requireNonNull( classNameToSearchFor );

		for( Class c : _classes ) {
			if( c.getName().equals( classNameToSearchFor ) || c.getSimpleName().equals( classNameToSearchFor ) ) {
				return c;
			}
		}

		// If the class isn't found by simple name, let's try constructing from a fully qualified class name.
		try {
			return Class.forName( classNameToSearchFor );
		}
		catch( ClassNotFoundException e ) {}

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
		m.put( "content", NGComponentContent.class.getSimpleName() );
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

	/**
	 * FIXME: This functionality should really be in a nicer location // Hugi 2021-11-20
	 */
	public static void stopPreviousDevelopmentInstance( int portNumber ) {
		try {
			final String urlString = String.format( "http://localhost:%s/wa/ng.appserver.privates.NGAdminAction/terminate", portNumber );
			new URL( urlString ).openConnection().getContent();
			Thread.sleep( 1000 );
		}
		catch( Throwable e ) {
			logger.info( "Terminated existing development instance" );
		}
	}
}