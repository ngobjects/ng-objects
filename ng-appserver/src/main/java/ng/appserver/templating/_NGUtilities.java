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
import ng.appserver.elements.NGConditional;
import ng.appserver.elements.NGForm;
import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGRepetition;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;
import ng.appserver.elements.NGTextField;
import x.junk.TestComponent;

/**
 * An abomination of a class that serves as a repository for temporary hacky stuff
 */

public class _NGUtilities {

	private static final Logger logger = LoggerFactory.getLogger( _NGUtilities.class );

	/**
	 * Classes registered to be searchable by classWithName()
	 */
	private static final List<Class<? extends NGElement>> _classes = new ArrayList<>();

	/**
	 * A mapping of shortcuts to element classes. For example, mapping of <wo:str /> to <wo:NGString />
	 */
	private static final Map<String, String> _tagShortcutMap = new HashMap<>();

	static {
		addClass( NGComponentContent.class, "content" );
		addClass( NGConditional.class, "if" );
		addClass( NGForm.class, "form" );
		addClass( NGString.class, "str" );
		addClass( NGImage.class, "img" );
		addClass( NGHyperlink.class, "link" );
		addClass( NGRepetition.class, "repetition" );
		addClass( NGStylesheet.class, "stylesheet" );
		addClass( NGTextField.class, "textfield" );
		addClass( TestComponent.class );
	}

	/**
	 * Add a class to make searchable by it's simpleName, full class name or any of the given shortcuts (for tags)
	 */
	public static void addClass( final Class<? extends NGElement> clazz, final String... shortcuts ) {
		_classes.add( clazz );

		for( String shortcut : shortcuts ) {
			_tagShortcutMap.put( shortcut, clazz.getSimpleName() );
		}
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

	/**
	 * Maps tag names to their dynamic element names
	 *
	 * FIXME: Definitely not the final home of this functionality // Hugi 2022-04-23
	 */
	public static Map<String, String> tagShortcutMap() {
		return _tagShortcutMap;
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