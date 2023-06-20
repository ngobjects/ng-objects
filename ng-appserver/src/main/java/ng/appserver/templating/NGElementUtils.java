package ng.appserver.templating;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGElement;
import ng.appserver.elements.NGActionURL;
import ng.appserver.elements.NGComponentContent;
import ng.appserver.elements.NGConditional;
import ng.appserver.elements.NGForm;
import ng.appserver.elements.NGGenericContainer;
import ng.appserver.elements.NGGenericElement;
import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGJavaScript;
import ng.appserver.elements.NGPopUpButton;
import ng.appserver.elements.NGRepetition;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;
import ng.appserver.elements.NGSubmitButton;
import ng.appserver.elements.NGSwitchComponent;
import ng.appserver.elements.NGTextField;
import x.junk.TestComponent;

/**
 * An abomination of a class that serves as a repository for temporary hacky stuff
 */

public class NGElementUtils {

	public static final Logger logger = LoggerFactory.getLogger( NGElementUtils.class );

	/**
	 * Classes registered to be searchable by classWithName()
	 */
	private static final List<Class<? extends NGElement>> _classes = new ArrayList<>();

	/**
	 * A mapping of shortcuts to element classes. For example, mapping of <wo:str /> to <wo:NGString />
	 */
	private static final Map<String, String> _shortcutToClassMap = new HashMap<>();

	static {
		addClass( NGActionURL.class, "actionURL" );
		addClass( NGComponentContent.class, "content" );
		addClass( NGConditional.class, "if" );
		addClass( NGForm.class, "form" );
		addClass( NGString.class, "str" );
		addClass( NGGenericContainer.class, "container" );
		addClass( NGGenericElement.class, "element" );
		addClass( NGImage.class, "img" );
		addClass( NGHyperlink.class, "link" );
		addClass( NGJavaScript.class, "script" );
		addClass( NGPopUpButton.class, "popup" );
		addClass( NGRepetition.class, "repetition" );
		addClass( NGSubmitButton.class, "submit" );
		addClass( NGStylesheet.class, "stylesheet" );
		addClass( NGSwitchComponent.class, "switch" );
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

	/**
	 * @return A new NGDynamicElement constructed using the given parameters
	 */
	public static <E extends NGElement> E createElement( final Class<E> elementClass, final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		final Class<?>[] parameterTypes = { String.class, Map.class, NGElement.class };
		final Object[] parameters = { name, associations, contentTemplate };

		try {
			final Constructor<E> constructor = elementClass.getDeclaredConstructor( parameterTypes );
			return constructor.newInstance( parameters );
		}
		catch( NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}
}