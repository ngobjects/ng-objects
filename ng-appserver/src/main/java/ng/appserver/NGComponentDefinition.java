package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.resources.NGResource;
import ng.appserver.templating.NGDeclarationFormatException;
import ng.appserver.templating.NGHTMLFormatException;
import ng.appserver.templating.NGTemplateParser;

/**
 * Stores information about the structure of the component.
 */

public class NGComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger( NGComponentDefinition.class );

	/**
	 * The referenced component's class
	 */
	private final Class<? extends NGComponent> _componentClass;

	/**
	 * FIXME: This is a temporary component definition cache, just to get us started // Hugi 2022-10-19
	 */
	private static Map<String, NGComponentDefinition> _componentDefinitionCache = new ConcurrentHashMap<>();

	/**
	 * Stores the parsed template if caching is enabled
	 */
	private NGElement _cachedTemplate;

	/**
	 * The canonical name of the component definition.
	 *
	 * This will never be the component's fully qualified class name (that information is available from componentClass()
	 */
	private final String _name;

	/**
	 * Class used to represent our own bookkeeping. Since template == null represents "not initialized yet", we use this to represent "initialized with no template available".
	 */
	private static class NoElement implements NGElement {}

	/**
	 * Template of components without a template
	 */
	private static final NoElement NO_ELEMENT = new NoElement();

	private NGComponentDefinition( final String componentName, final Class<? extends NGComponent> componentClass ) {
		Objects.requireNonNull( componentName );
		Objects.requireNonNull( componentClass );
		_componentClass = componentClass;
		_name = componentName;
	}

	/**
	 * @return The cached component with the given name
	 */
	private static NGComponentDefinition _cachedComponentDefinition( final String componentName ) {
		Objects.requireNonNull( componentName );
		return _componentDefinitionCache.get( componentName );
	}

	/**
	 *  FIXME: Temp caching implementation
	 */
	private static boolean _cachingEnabled() {
		return NGApplication.application().cachingEnabled();
	}

	/**
	 * @return The component definition for the given name.
	 */
	public static NGComponentDefinition get( final String componentName ) {
		Objects.requireNonNull( componentName );
		return get( componentName, null );
	}

	/**
	 * @return The component definition for the given class.
	 */
	public static NGComponentDefinition get( final Class<? extends NGComponent> componentClass ) {
		Objects.requireNonNull( componentClass );
		return get( null, componentClass );
	}

	/**
	 * @return a component definition by either name OR class (never both)
	 */
	private static NGComponentDefinition get( String componentName, Class<? extends NGComponent> componentClass ) {

		// You're not allowed to pass in both componentName and componentClass
		if( componentName != null && componentClass != null ) {
			throw new IllegalArgumentException( "You can't specify both componentName and componentClass, just either one" );
		}

		// You must pass in either a name or a class
		if( componentName == null && componentClass == null ) {
			throw new IllegalArgumentException( "You must specify either componentName or componentClass" );
		}

		if( componentName == null ) {
			componentName = componentClass.getSimpleName();
		}

		if( _cachingEnabled() ) {
			NGComponentDefinition cached = _cachedComponentDefinition( componentName );

			if( cached != null ) {
				return cached;
			}
		}

		if( componentClass == null ) {
			componentClass = NGElementUtils.classWithNameNullIfNotFound( componentName );

			if( componentClass == null ) {
				componentClass = NGComponent.class;
			}
		}

		final NGComponentDefinition newComponentDefinition = new NGComponentDefinition( componentName, componentClass );

		if( newComponentDefinition.isClassless() && !newComponentDefinition.hasTemplate() ) {
			throw new IllegalArgumentException( "Component '%s' does not exist (a component must have either a class or a template, usually both)".formatted( componentName ) );
		}

		if( _cachingEnabled() ) {
			_componentDefinitionCache.put( componentName, newComponentDefinition );
		}

		return newComponentDefinition;
	}

	/**
	 * @return The name of the component definition. For class-based component, this will correspond to the component class's simpleName
	 */
	public String name() {
		return _name;
	}

	/**
	 * @return A new component of the given class in the given context
	 *
	 * CHECKME: In the case of stateless components, we are going to want to share an already cached instance // Hugi 2022-10-19
	 */
	public NGComponent componentInstanceInContext( final NGContext context ) {
		Objects.requireNonNull( context );

		try {
			final NGComponent newComponentInstance = _componentClass.getConstructor( NGContext.class ).newInstance( context );
			newComponentInstance._setComponentDefinition( this ); // FIXME: Feel like this is ugly as all hell, the _componentDefinition variable should not be exposed
			return newComponentInstance;
		}
		catch( InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * @return A new component reference to insert into a template being rendered
	 */
	public NGComponentReference componentReferenceWithAssociations( final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		return new NGComponentReference( name(), associations, contentTemplate );
	}

	/**
	 * @return The parsed template for this component
	 */
	public NGElement template() {
		if( _cachingEnabled() ) {
			if( _cachedTemplate == null ) {
				_cachedTemplate = _loadTemplate();
			}

			return _cachedTemplate;
		}

		return _loadTemplate();
	}

	/**
	 * @return A component template by loading the component's template files and parsing them
	 */
	private NGElement _loadTemplate() {
		try {
			// Let's try first for the traditional template
			Optional<String> htmlTemplateStringOptional = loadHTMLStringFromTemplateFolder( name() );
			Optional<String> wodStringOptional = loadWODStringFromTemplateFolder( name() );

			// If that fails, let's go for the single file html template
			if( htmlTemplateStringOptional.isEmpty() ) {
				final Optional<NGResource> htmlTemplate = NGApplication.application().resourceManager().obtainComponentTemplateResourceSearchingAllNamespaces( name() + ".html" );

				if( htmlTemplate.isPresent() ) {
					htmlTemplateStringOptional = Optional.of( new String( htmlTemplate.get().bytes(), StandardCharsets.UTF_8 ) );
				}
			}

			// If no html template string has been loaded, no template exists.
			// CHECKME: We might want to fail here since a non-existent template at load time is almost definitely an error // Hugi 2023-08-27
			if( htmlTemplateStringOptional.isEmpty() ) {
				logger.warn( "Component template '%s' not found".formatted( name() ) );
				return NO_ELEMENT;
			}

			return NGTemplateParser.parse( htmlTemplateStringOptional.get(), wodStringOptional.orElse( "" ) );
		}
		catch( ClassNotFoundException | NGDeclarationFormatException | NGHTMLFormatException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * @return true if this component does not have it's own class representation
	 */
	private boolean isClassless() {
		return _componentClass == NGComponent.class;
	}

	/**
	 * @return true if this component has not loaded template
	 */
	private boolean hasTemplate() {
		return !(template() instanceof NoElement);
	}

	/**
	 * @return The HTML template for the named component
	 */
	private static Optional<String> loadHTMLStringFromTemplateFolder( final String templateName ) {
		Objects.requireNonNull( templateName );
		return loadStringFromTemplateFolder( templateName, "html" );
	}

	/**
	 * @return The WOD template for the named component
	 */
	private static Optional<String> loadWODStringFromTemplateFolder( final String templateName ) {
		Objects.requireNonNull( templateName );
		return loadStringFromTemplateFolder( templateName, "wod" );
	}

	/**
	 * @return The string template for the named component
	 */
	private static Optional<String> loadStringFromTemplateFolder( final String templateName, final String extension ) {
		Objects.requireNonNull( templateName );
		Objects.requireNonNull( extension );

		final String htmlTemplateFilename = templateName + ".wo/" + templateName + "." + extension;

		final Optional<NGResource> templateBytes = NGApplication.application().resourceManager().obtainComponentTemplateResourceSearchingAllNamespaces( htmlTemplateFilename );

		if( templateBytes.isEmpty() ) {
			return Optional.empty();
		}

		return Optional.of( new String( templateBytes.get().bytes(), StandardCharsets.UTF_8 ) );
	}
}