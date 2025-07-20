package ng.appserver.templating;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGContext;
import ng.appserver.elements.NGComponentReference;
import ng.appserver.templating.assications.NGAssociation;

/**
 * Manages access to and location of dynamic elements and components registered with a project
 */

public class NGElementManager {

	/**
	 * To ease the porting of older templates to this system we allow unnamespaced elements. This is done
	 * by designating the namespace "wo" a "magic namespace" which when used for locating elements means "search every namespace".
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
		return componentInstance( definition, context );
	}

	/**
	 * @return A new instance of [componentClass] in the given [context]
	 */
	@SuppressWarnings("unchecked") // Our cast to the component class is fine
	public <E extends NGComponent> E pageWithName( final Class<E> componentClass, final NGContext context ) {
		Objects.requireNonNull( componentClass, "'componentClass' must not be null. I can't create components from nothing." );
		Objects.requireNonNull( context, "'context' must not be null. What's life without context?" );

		final NGComponentDefinition definition = componentDefinition( componentClass );
		return (E)componentInstance( definition, context );
	}

	/**
	 * @return A new instance of [componentDefinition] in the given [context]
	 */
	private NGComponent componentInstance( final NGComponentDefinition componentDefinition, final NGContext context ) {
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
	 * FIXME: "Tag lookup" is a separate (cacheable) task from "Tag construction". Separate the two // Hugi 2025-04-19
	 * FIXME: We're missing a cache for dynamic element name resolution // Hugi 2025-03-05
	 * FIXME: We are going to have to support namespace aliases // Hugi 2025-03-20
	 * FIXME: We are going to have to support recursion when looking for "tag aliases". I.e. you should be able to look up "teh alias of an alias" // Hugi 2025-03-20
	 * FIXME: Tag aliasing needs to be namespace aware in general // Hugi 2025-03-20
	 */
	public NGDynamicElement dynamicElementWithName( final String namespace, final String elementIdentifier, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( elementIdentifier );
		Objects.requireNonNull( associations );

		// First we're going to check if we have a tag alias present.
		final String elementName = resolveTagName( elementIdentifier );

		// Check if we can find a class representing the element we're going to render.
		final Class<? extends NGElement> elementClass = classWithSimpleNameNullIfNotFound( elementName );

		// If we don't find a class for the element, we're going to try going down the route of a classless component.
		if( elementClass == null ) {
			final NGComponentDefinition componentDefinition = componentDefinition( elementName );
			return createComponentReference( componentDefinition, associations, contentTemplate );
		}

		// First we check if this is a dynamic element
		if( NGDynamicElement.class.isAssignableFrom( elementClass ) ) {
			return createDynamicElementInstance( (Class<? extends NGDynamicElement>)elementClass, elementName, associations, contentTemplate );
		}

		// If it's not an element, let's move on to creating a component reference instead
		if( NGComponent.class.isAssignableFrom( elementClass ) ) {
			final NGComponentDefinition componentDefinition = componentDefinition( (Class<? extends NGComponent>)elementClass );
			return createComponentReference( componentDefinition, associations, contentTemplate );
		}

		// We should never end up here unless the element name resolves to some random class
		throw new NGElementNotFoundException( "Class '%s' (obtained for element identifier %s) does not extend NGComponent or NGDynamicElement".formatted( elementClass, elementIdentifier ), elementName );
	}

	/**
	 * @return A new NGDynamicElement constructed using the given parameters. Really just a shortcut for invoking a dynamic element class' constructor via reflection.
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
	 * @return A new component reference element for inserting into a template being rendered
	 */
	private static NGComponentReference createComponentReference( final NGComponentDefinition componentDefinition, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		return NGComponentReference.of( componentDefinition, associations, contentTemplate );
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
	 * Register a provider of element(s)
	 */
	public void registerElementProvider( final ElementProvider elementProvider ) {
		switch( elementProvider ) {
			case ElementByClass o -> registerElementClass( o.namespace(), o.elementClass(), o.tagNames() );
			case ElementsByPackage o -> registerElementPackage( o.namespace(), o.packageName() );
			case ElementAliases o -> throw new IllegalArgumentException( "Not implemented" );
		}
	}

	/**
	 * Registers an element class for use in the application
	 */
	private void registerElementClass( final String namespace, final Class<?> elementClass, String... tagNames ) {
		_elementClasses.put( elementClass.getSimpleName(), elementClass );

		for( final String tagName : tagNames ) {
			_elementTagNames.put( tagName, elementClass.getSimpleName() );
		}
	}

	/**
	 * Registers an element class for use in the application
	 */
	private void registerElementPackage( final String namespace, final String packageName ) {
		_elementPackages.add( packageName );
	}

	/**
	 * @return A class with the given simpleClassName
	 */
	public Class classWithSimpleNameNullIfNotFound( String simpleClassName ) {

		Objects.requireNonNull( simpleClassName );

		final Class<?> elementClass = _elementClasses.get( simpleClassName );

		if( elementClass != null ) {
			return elementClass;
		}

		for( final String packageName : _elementPackages ) {
			try {
				final String className = packageName + "." + simpleClassName;
				return Class.forName( className );
			}
			catch( ClassNotFoundException e ) {}
		}

		return null;
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
	 * @return The actual name of the given tagName, obtained by resolving any tag aliases
	 */
	public String resolveTagName( final String elementIdentifier ) {
		return elementTagNames().getOrDefault( elementIdentifier, elementIdentifier );
	}

	/**
	 * An interface that declares methods used by the framework
	 */
	public sealed interface ElementProvider permits ElementByClass, ElementsByPackage, ElementAliases {}

	public record ElementByClass( String namespace, Class<? extends NGElement> elementClass, String[] tagNames ) implements ElementProvider {}

	public record ElementsByPackage( String namespace, String packageName ) implements ElementProvider {}

	public record ElementAliases( String tagName, String[] tagAliases ) implements ElementProvider {}
}