package ng.appserver.templating;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGComponentDefinition;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGElementUtils;

public class NGElementManager {

	/**
	 * @return The named component, where [componentName] can be either the component's simple class name or full class name.
	 */
	public NGComponent pageWithName( final String componentName, final NGContext context ) {
		Objects.requireNonNull( componentName, "'componentName' must not be null. I can't create components from nothing." );
		Objects.requireNonNull( context, "'context' must not be null. What's life without context?" );

		final NGComponentDefinition definition = _componentDefinition( componentName );
		return pageWithName( definition, context );
	}

	/**
	 * @return A new instance of [componentClass] in the given [context]
	 */
	@SuppressWarnings("unchecked") // Our cast to the component class is fine
	public <E extends NGComponent> E pageWithName( final Class<E> componentClass, final NGContext context ) {
		Objects.requireNonNull( componentClass, "'componentClass' must not be null. I can't create components from nothing." );
		Objects.requireNonNull( context, "'context' must not be null. What's life without context?" );

		final NGComponentDefinition definition = _componentDefinition( componentClass );
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
	 *
	 * FIXME: This should not be static // Hugi 2023-04-14
	 */
	private static NGComponentDefinition _componentDefinition( final Class<? extends NGComponent> componentClass ) {
		Objects.requireNonNull( componentClass );
		return NGComponentDefinition.get( componentClass );
	}

	/**
	 * @return The componentDefinition corresponding to the named NGComponent
	 *
	 * FIXME: This should not be static // Hugi 2023-04-14
	 */
	public static NGComponentDefinition _componentDefinition( final String componentName ) {
		Objects.requireNonNull( componentName );
		return NGComponentDefinition.get( componentName );
	}

	/**
	 * FIXME: This should not be static // Hugi 2023-04-14
	 *
	 * @param name The name identifying what element we're getting
	 * @param associations Associations used to bind the generated element to it's parent
	 * @param contentTemplate The content wrapped by the element (if a container element)
	 *
	 * @return An instance of the named dynamic element. This can be a classless component (in which case it's the template name), a simple class name or a full class name
	 */
	public static NGElement dynamicElementWithName( final String elementIdentifier, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		Objects.requireNonNull( elementIdentifier );
		Objects.requireNonNull( associations );

		// First we're going to check if we have a tag alias present.
		final String elementName = NGElementUtils.tagShortcutMap().getOrDefault( elementIdentifier, elementIdentifier );

		// Check if we can find a class representing the element we're going to render.
		final Class<? extends NGElement> elementClass = NGElementUtils.classWithNameNullIfNotFound( elementName );

		// If we don't find a class for the element, we're going to try going down the route of a classless component.
		if( elementClass == null ) {
			final NGComponentDefinition componentDefinition = _componentDefinition( elementName );
			return componentDefinition.componentReferenceWithAssociations( associations, contentTemplate );
		}

		// First we check if this is a dynamic element
		if( NGDynamicElement.class.isAssignableFrom( elementClass ) ) {
			return createDynamicElementInstance( elementClass, elementName, associations, contentTemplate );
		}

		// If it's not an element, let's move on to creating a component reference instead
		if( NGComponent.class.isAssignableFrom( elementClass ) ) {
			final NGComponentDefinition componentDefinition = _componentDefinition( (Class<? extends NGComponent>)elementClass );
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
	private static <E extends NGElement> E createDynamicElementInstance( final Class<E> elementClass, final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
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