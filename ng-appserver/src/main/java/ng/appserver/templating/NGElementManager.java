package ng.appserver.templating;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGContext;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.elements.NGComponentReference;

/**
 * Manages access to and location of dynamic elements and components registered with a project
 */

public class NGElementManager {

	private static final Logger logger = LoggerFactory.getLogger( NGElementManager.class );

	/**
	 * Classpath resource declaring tag aliases: each entry is {@code tagName = element}, where the
	 * element is a registered element's simple class name (or another alias — resolution is
	 * recursive). ng-appserver ships the framework's own registry in this file; frameworks and
	 * applications contribute more by shipping a same-named file in their resources. The template
	 * editor reads exactly these files, so tags resolve identically in the app and in the IDE.
	 */
	public static final String TAG_ALIASES_RESOURCE = "parsley-tag-aliases.properties";

	public NGElementManager() {
		// Declared aliases are loaded first, so a plugin's explicit code registration
		// (Elements.elementClass( cls, "tag" )) can still override a declared one.
		loadTagAliasResources();
	}

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
			registerTagAlias( tagName, elementClass.getSimpleName() );
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
	 * @return The actual name of the given tagName, obtained by resolving tag aliases recursively to a
	 * fixed point ({@code str -> NGString}, or {@code myStr -> str -> NGString}). A name with no alias
	 * resolves to itself. A cycle is broken defensively (warned, last name returned) so a bad
	 * declaration can't hang rendering.
	 */
	public String resolveTagName( final String elementIdentifier ) {
		String current = elementIdentifier;
		final Set<String> seen = new HashSet<>();
		seen.add( current );
		String next;

		while( (next = _elementTagNames.get( current )) != null ) {
			if( !seen.add( next ) ) {
				logger.warn( "Cycle detected resolving tag alias for '{}' (at '{}'); stopping.", elementIdentifier, next );
				break;
			}
			current = next;
		}

		return current;
	}

	/**
	 * Registers (or overrides) a tag alias: {@code tagName} will resolve to {@code target} (an element's
	 * simple class name, or another alias).
	 */
	public void registerTagAlias( final String tagName, final String target ) {
		Objects.requireNonNull( tagName );
		Objects.requireNonNull( target );
		_elementTagNames.put( tagName, target );
	}

	/**
	 * Loads every {@link #TAG_ALIASES_RESOURCE} on the classpath into the tag-name map. When two files
	 * map the same tag to different targets there's no reliable way to honour classpath order from
	 * runtime information ({@code ClassLoader.getResources} order is unspecified), so the first
	 * declaration stands and the conflicting one is ignored with a warning — the same rule the
	 * Parsley tag registry and the template editor apply, so all three agree.
	 */
	private void loadTagAliasResources() {
		try {
			final Enumeration<URL> resources = NGElementManager.class.getClassLoader().getResources( TAG_ALIASES_RESOURCE );

			while( resources.hasMoreElements() ) {
				final URL url = resources.nextElement();

				try( InputStream in = url.openStream() ) {
					final Properties props = new Properties();
					props.load( in );

					for( final String alias : props.stringPropertyNames() ) {
						final String target = props.getProperty( alias );

						if( alias.isBlank() || target == null || target.isBlank() ) {
							continue;
						}

						final String existing = _elementTagNames.get( alias.trim() );

						if( existing != null && !existing.equals( target.trim() ) ) {
							logger.warn( "Ignoring conflicting tag alias '{}' -> '{}' from {}; already registered as '{}' -> '{}'", alias, target, url, alias, existing );
							continue;
						}

						_elementTagNames.put( alias.trim(), target.trim() );
					}

					logger.debug( "Loaded tag aliases from {}", url );
				}
				catch( final IOException e ) {
					logger.warn( "Failed to read tag alias resource {}", url, e );
				}
			}
		}
		catch( final IOException e ) {
			logger.warn( "Failed to enumerate {} resources on the classpath", TAG_ALIASES_RESOURCE, e );
		}
	}

	/**
	 * An interface that declares methods used by the framework
	 */
	public sealed interface ElementProvider permits ElementByClass, ElementsByPackage, ElementAliases {}

	public record ElementByClass( String namespace, Class<? extends NGElement> elementClass, String[] tagNames ) implements ElementProvider {}

	public record ElementsByPackage( String namespace, String packageName ) implements ElementProvider {}

	public record ElementAliases( String tagName, String[] tagAliases ) implements ElementProvider {}
}