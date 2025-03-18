package ng.appserver.templating;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGContext;
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
import ng.appserver.elements.NGResourceURL;
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
import ng.appserver.templating.assications.NGAssociation;

/**
 * Manages access to and location of dynamic elements and components registered with a project
 */

public class NGElementManager {

	/**
	 * I WO, elements are not namespaced. To help porting older templates, we therefore allow unnamespaced elements
	 * by designating the namespace "wo" a "magic namespace" which when used for locating elements means searching every namespace.
	 *
	 * This is probably/hopefully temporary.
	 */
	public static final String GLOBAL_UNNAMESPACED_NAMESPACE = "wo";

	/**
	 * @return The named component, where [componentName] can be either the component's simple class name or full class name.
	 */
	public NGComponent pageWithName( final String componentName, final NGContext context ) {
		Objects.requireNonNull( componentName, "'componentName' must not be null. I can't create components from nothing." );
		Objects.requireNonNull( context, "'context' must not be null. What's life without context?" );

		final NGComponentDefinition definition = componentDefinition( componentName );
		return pageWithName( definition, context );
	}

	/**
	 * @return A new instance of [componentClass] in the given [context]
	 */
	@SuppressWarnings("unchecked") // Our cast to the component class is fine
	public <E extends NGComponent> E pageWithName( final Class<E> componentClass, final NGContext context ) {
		Objects.requireNonNull( componentClass, "'componentClass' must not be null. I can't create components from nothing." );
		Objects.requireNonNull( context, "'context' must not be null. What's life without context?" );

		final NGComponentDefinition definition = componentDefinition( componentClass );
		return (E)pageWithName( definition, context );
	}

	/**
	 * @return A new instance of [componentDefinition] in the given [context]
	 */
	private NGComponent pageWithName( final NGComponentDefinition componentDefinition, final NGContext context ) {
		Objects.requireNonNull( componentDefinition );
		Objects.requireNonNull( context );

		return componentDefinition.componentInstanceInContext( context );
	}

	/**
	 * @return The componentDefinition corresponding to the given NGComponent class.
	 */
	private static NGComponentDefinition componentDefinition( final Class<? extends NGComponent> componentClass ) {
		Objects.requireNonNull( componentClass );
		return NGComponentDefinition.get( componentClass );
	}

	/**
	 * @return The componentDefinition corresponding to the named NGComponent
	 */
	private static NGComponentDefinition componentDefinition( final String componentName ) {
		Objects.requireNonNull( componentName );
		return NGComponentDefinition.get( componentName );
	}

	/**
	 * @param name The name identifying what element we're getting
	 * @param associations Associations used to bind the generated element to it's parent
	 * @param contentTemplate The content wrapped by the element (if a container element)
	 *
	 * @return An instance of the named dynamic element. This can be a classless component (in which case it's the template name), a simple class name or a full class name
	 *
	 * FIXME: We're missing a cache for dynamic element name resolution // Hugi 2025-03-05
	 */
	public NGDynamicElement dynamicElementWithName( final String namespace, final String elementIdentifier, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( elementIdentifier );
		Objects.requireNonNull( associations );

		// First we're going to check if we have a tag alias present.
		final String elementName = elementTagNames().getOrDefault( elementIdentifier, elementIdentifier );

		// Check if we can find a class representing the element we're going to render.
		final Class<? extends NGElement> elementClass = classWithNameNullIfNotFound( elementName );

		// If we don't find a class for the element, we're going to try going down the route of a classless component.
		if( elementClass == null ) {
			final NGComponentDefinition componentDefinition = componentDefinition( elementName );
			return componentDefinition.componentReferenceWithAssociations( associations, contentTemplate );
		}

		// First we check if this is a dynamic element
		if( NGDynamicElement.class.isAssignableFrom( elementClass ) ) {
			return createDynamicElementInstance( (Class<? extends NGDynamicElement>)elementClass, elementName, associations, contentTemplate );
		}

		// If it's not an element, let's move on to creating a component reference instead
		if( NGComponent.class.isAssignableFrom( elementClass ) ) {
			final NGComponentDefinition componentDefinition = componentDefinition( (Class<? extends NGComponent>)elementClass );
			return componentDefinition.componentReferenceWithAssociations( associations, contentTemplate );
		}

		// We should never end up here unless we got an incorrect/non-existent element name
		throw new NGElementNotFoundException( "I could not construct a dynamic element named '%s'".formatted( elementName ), elementName );
	}

	/**
	 * @return A new NGDynamicElement constructed using the given parameters
	 *
	 * Really just a shortcut for invoking a dynamic element class' constructor via reflection.
	 */
	private static <E extends NGDynamicElement> E createDynamicElementInstance( final Class<E> elementClass, final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
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

	/**
	 * Packages that we look for element classes in
	 */
	private final List<String> _elementPackages = new ArrayList<>();

	/**
	 * Explicitly registered element classes
	 */
	private final Map<String, Class<?>> _elementClasses = new HashMap<>();

	/**
	 * A mapping of shortcuts to element classes. For example, mapping of <wo:str /> to <wo:NGString />
	 */
	private final Map<String, String> _elementTagNames = new HashMap<>();

	/**
	 * FIXME: Allows us to invoke registerElementClass without a namespace. Temporary while we're working on namespaces
	 */
	@Deprecated
	public void registerElementClass( final Class<?> elementClass, String... tagNames ) {
		registerElementClass( GLOBAL_UNNAMESPACED_NAMESPACE, elementClass, tagNames );
	}

	/**
	 * FIXME: Allows us to invoke registerElementPackage without a namespace. Temporary while we're working on namespaces
	 */
	@Deprecated
	public void registerElementPackage( final String packageName ) {
		registerElementPackage( GLOBAL_UNNAMESPACED_NAMESPACE, packageName );
	}

	/**
	 * Registers an element class for use in the application
	 */
	public void registerElementClass( final String namespace, final Class<?> elementClass, String... tagNames ) {
		_elementClasses.put( elementClass.getSimpleName(), elementClass );

		for( final String tagName : tagNames ) {
			_elementTagNames.put( tagName, elementClass.getSimpleName() );
		}
	}

	/**
	 * Registers an element class for use in the application
	 */
	public void registerElementPackage( final String namespace, final String packageName ) {
		_elementPackages.add( packageName );
	}

	/**
	 * @return A class matching classNameToSearch for. Searches by fully qualified class name and simpleName.
	 */
	public Class classWithNameNullIfNotFound( String classNameToSearchFor ) {
		Objects.requireNonNull( classNameToSearchFor );

		final Class<?> elementClass = _elementClasses.get( classNameToSearchFor );

		if( elementClass != null ) {
			return elementClass;
		}

		for( String packageName : _elementPackages ) {
			try {
				final String className = packageName + "." + classNameToSearchFor;
				return Class.forName( className );
			}
			catch( ClassNotFoundException e ) {}
		}

		return null;
		// throw new RuntimeException( "Class not found: " + classNameToSearchFor );
	}

	/**
	 * Maps tag names to their dynamic element names
	 *
	 * FIXME: Definitely not the final home of this functionality // Hugi 2022-04-23
	 */
	public Map<String, String> elementTagNames() {
		return _elementTagNames;
	}

	/**
	 * FIXME: Delete this method once we've moved the initialization of the framework elements to it's own module // Hugi 2025-03-16
	 */
	@Deprecated
	public void registerFrameworkElementClasses() {
		registerElementClass( NGActionURL.class, "actionURL" );
		registerElementClass( AjaxUpdateContainer.class, "auc" );
		registerElementClass( AjaxUpdateLink.class, "aul" );
		registerElementClass( AjaxObserveField.class, "aof" );
		registerElementClass( AjaxSubmitButton.class, "asb" );
		registerElementClass( NGBrowser.class, "browser" );
		registerElementClass( NGComponentContent.class, "content" );
		registerElementClass( NGConditional.class, "if" );
		registerElementClass( NGForm.class, "form" );
		registerElementClass( NGString.class, "str" );
		registerElementClass( NGGenericContainer.class, "container" );
		registerElementClass( NGGenericElement.class, "element" );
		registerElementClass( NGImage.class, "img" );
		registerElementClass( NGHyperlink.class, "link" );
		registerElementClass( NGJavaScript.class, "script" );
		registerElementClass( NGPopUpButton.class, "popUpButton" ); // CHECKME: We might want to consider just naming this "popup"
		registerElementClass( NGRepetition.class, "repetition" );
		registerElementClass( NGResourceURL.class, "resourceURL" );
		registerElementClass( NGSubmitButton.class, "submit" );
		registerElementClass( NGStylesheet.class, "stylesheet" );
		registerElementClass( NGSwitchComponent.class, "switch" );
		registerElementClass( NGText.class, "text" );
		registerElementClass( NGTextField.class, "textfield" );
	}
}