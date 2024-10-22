package ng.appserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.elements.NGActionURL;
import ng.appserver.elements.NGBrowser;
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
import ng.appserver.elements.NGText;
import ng.appserver.elements.NGTextField;
import ng.appserver.elements.ajax.AjaxObserveField;
import ng.appserver.elements.ajax.AjaxSubmitButton;
import ng.appserver.elements.ajax.AjaxUpdateContainer;
import ng.appserver.elements.ajax.AjaxUpdateLink;

/**
 * An abomination of a class that serves as a repository for temporary hacky stuff
 */

@Deprecated
public class NGElementUtils {

	private static final Logger logger = LoggerFactory.getLogger( NGElementUtils.class );

	/**
	 * Packages that we look for component classes inside
	 */
	private static List<String> _packages = new ArrayList<>();

	/**
	 * Classes registered to be searchable by classWithName()
	 */
	private static final List<Class<?>> _classes = new ArrayList<>();

	/**
	 * A mapping of shortcuts to element classes. For example, mapping of <wo:str /> to <wo:NGString />
	 */
	private static final Map<String, String> _shortcutToClassMap = new HashMap<>();

	static {
		addClass( NGActionURL.class, "actionURL" );
		addClass( AjaxUpdateContainer.class, "auc" );
		addClass( AjaxUpdateLink.class, "aul" );
		addClass( AjaxObserveField.class, "aof" );
		addClass( AjaxSubmitButton.class, "asb" );
		addClass( NGBrowser.class, "browser" );
		addClass( NGComponentContent.class, "content" );
		addClass( NGConditional.class, "if" );
		addClass( NGForm.class, "form" );
		addClass( NGString.class, "str" );
		addClass( NGGenericContainer.class, "container" );
		addClass( NGGenericElement.class, "element" );
		addClass( NGImage.class, "img" );
		addClass( NGHyperlink.class, "link" );
		addClass( NGJavaScript.class, "script" );
		addClass( NGPopUpButton.class, "popUpButton" ); // CHECKME: We might want to consider just naming this "popup"
		addClass( NGRepetition.class, "repetition" );
		addClass( NGSubmitButton.class, "submit" );
		addClass( NGStylesheet.class, "stylesheet" );
		addClass( NGSwitchComponent.class, "switch" );
		addClass( NGText.class, "text" );
		addClass( NGTextField.class, "textfield" );
	}

	/**
	 * Add a class to make searchable by it's simpleName, full class name or any of the given shortcuts (for tags)
	 */
	public static void addClass( final Class<?> clazz, final String... shortcuts ) {
		_classes.add( clazz );

		for( String shortcut : shortcuts ) {
			_shortcutToClassMap.put( shortcut, clazz.getSimpleName() );
		}
	}

	public static void addPackage( final String packageName ) {
		_packages.add( packageName );
	}

	/**
	 * @return A class matching classNameToSearch for. Searches by fully qualified class name and simpleName.
	 */
	private static Class<?> classWithName( String classNameToSearchFor ) {
		Objects.requireNonNull( classNameToSearchFor );

		logger.debug( "Searching for class '{}'", classNameToSearchFor );

		for( Class<?> c : _classes ) {
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

		for( String packageName : _packages ) {
			try {
				final String className = packageName + "." + classNameToSearchFor;
				return Class.forName( className );
			}
			catch( ClassNotFoundException e ) {}
		}

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
}