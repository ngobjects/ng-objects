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

import ng.appserver.NGApplication;
import ng.appserver.NGElement;
import ng.appserver.elements.NGComponentContent;
import ng.appserver.elements.NGConditional;
import ng.appserver.elements.NGForm;
import ng.appserver.elements.NGGenericContainer;
import ng.appserver.elements.NGGenericElement;
import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGJavaScript;
import ng.appserver.elements.NGRepetition;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;
import ng.appserver.elements.NGSubmitButton;
import ng.appserver.elements.NGTextField;
import x.junk.TestComponent;

/**
 * An abomination of a class that serves as a repository for temporary hacky stuff
 */

public class NGElementUtils {

	private static final Logger logger = LoggerFactory.getLogger( NGElementUtils.class );

	/**
	 * Classes registered to be searchable by classWithName()
	 */
	private static final List<Class<? extends NGElement>> _classes = new ArrayList<>();

	/**
	 * A mapping of shortcuts to element classes. For example, mapping of <wo:str /> to <wo:NGString />
	 */
	private static final Map<String, String> _shortcutToClassMap = new HashMap<>();

	static {
		addClass( NGComponentContent.class, "content" );
		addClass( NGConditional.class, "if" );
		addClass( NGForm.class, "form" );
		addClass( NGString.class, "str" );
		addClass( NGGenericContainer.class, "container" );
		addClass( NGGenericElement.class, "element" );
		addClass( NGImage.class, "img" );
		addClass( NGHyperlink.class, "link" );
		addClass( NGJavaScript.class, "script" );
		addClass( NGRepetition.class, "repetition" );
		addClass( NGSubmitButton.class, "submit" );
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
			_shortcutToClassMap.put( shortcut, clazz.getSimpleName() );
		}
	}

	/**
	 * @return A class matching classNameToSearch for. Searches by fully qualified class name and simpleName.
	 */
	public static Class classWithName( String classNameToSearchFor ) {
		Objects.requireNonNull( classNameToSearchFor );

		logger.debug( "Searching for class '{}'", classNameToSearchFor );

		for( Class c : _classes ) {
			if( c.getName().equals( classNameToSearchFor ) || c.getSimpleName().equals( classNameToSearchFor ) ) {
				return c;
			}
		}

		// If the class isn't found by simple name, let's try constructing from a fully qualified class name.
		try {
			logger.debug( "Did not find class '{}'. Trying Class.forName()", classNameToSearchFor );
			return Class.forName( classNameToSearchFor );
		}
		catch( ClassNotFoundException e ) {}

		// FIXME: Finally, and this is horrible, we're going to look for a component class inside the same package as the application class // Hugi 2022-10-10
		try {
			final String className = NGApplication.application().getClass().getPackageName() + "." + classNameToSearchFor;
			return Class.forName( className );
		}
		catch( ClassNotFoundException e ) {}

		throw new RuntimeException( "Class not found: " + classNameToSearchFor );
	}

	/**
	 * FIXME: This is horrible
	 */
	public static Class classWithNameNullIfNotFound( String classNameToSearchFor ) {
		try {
			return classWithName( classNameToSearchFor );
		}
		catch( RuntimeException e ) {
			return null;
		}
	}

	/**
	 * Maps tag names to their dynamic element names
	 *
	 * FIXME: Definitely not the final home of this functionality // Hugi 2022-04-23
	 */
	public static Map<String, String> tagShortcutMap() {
		return _shortcutToClassMap;
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

	private static boolean alreadyTriedStopping = false;

	/**
	 * FIXME: This functionality should really be in a nicer location // Hugi 2021-11-20
	 * FIXME: Kill an existing WO application if that's what's blocking the instance
	 */
	public static void stopPreviousDevelopmentInstance( int portNumber ) {
		if( alreadyTriedStopping ) {
			logger.info( "We've already unsuccessfully tried stopping a previous application instance, and it didn't work. No sense trying again. Exiting" );
			NGApplication.application().terminate();
		}

		try {
			final String urlString = String.format( "http://localhost:%s/wa/ng.appserver.privates.NGAdminAction/terminate", portNumber );
			new URL( urlString ).openConnection().getContent();
			Thread.sleep( 1000 );
			alreadyTriedStopping = true;
		}
		catch( Throwable e ) {
			logger.info( "Terminated existing development instance" );
		}
	}

	/**
	 * @return true if the given object is "truthy" for conditionals
	 *
	 * The conditions under which that is are
	 * - boolean true
	 * - a number that's exactly zero
	 * - null
	 */
	public static boolean isTruthy( Object object ) {

		if( object == null ) {
			return false;
		}

		if( object instanceof Boolean b ) {
			return b;
		}

		if( object instanceof Number n ) {
			return n.doubleValue() != 0;
		}

		return true;
	}
}