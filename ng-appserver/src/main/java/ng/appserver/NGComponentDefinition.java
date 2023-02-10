package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.privates.NGResourceLoader;
import ng.appserver.templating.NGDeclarationFormatException;
import ng.appserver.templating.NGElementUtils;
import ng.appserver.templating.NGHTMLFormatException;
import ng.appserver.templating.NGTemplateParser;

/**
 * FIXME: We need to decide what parts of the component name/class name we're going to keep around // Hugi 2022-04-22
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
	private static Map<String, NGComponentDefinition> _componentDefinitionCache = new HashMap<>();

	/**
	 * Stores the parsed template if caching is enabled
	 */
	private NGElement _cachedTemplate;

	/**
	 * The canonical name of the component definition.
	 *
	 * - In the case of class-based components, this will be the component's fully qualified class name
	 * - In the case of classless component, this will be the template's filename (excluding the file's suffix)
	 */
	private final String _name;

	private NGComponentDefinition( final String componentName, final Class<? extends NGComponent> componentClass ) {
		Objects.requireNonNull( componentName );
		Objects.requireNonNull( componentClass );
		_componentClass = componentClass;
		_name = componentName;
	}

	/**
	 * @return The cached component with the given name
	 *
	 * FIXME: Temp caching implementation
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
	 * @return The component definition for the given class.
	 */
	public static NGComponentDefinition get( final String componentName ) {
		Objects.requireNonNull( componentName );
		return get( componentName, null );
	}

	/**
	 * @return The component definition for the given name.
	 */
	public static NGComponentDefinition get( final Class<? extends NGComponent> componentClass ) {
		Objects.requireNonNull( componentClass );
		return get( null, componentClass );
	}

	/**
	 * @return a component definition by either name OR class (never both)
	 *
	 * FIXME: We should be failing here if the componentDefinition has neither a class nor a template (Since that's a non-existant component) // Hugi 2023-02-03
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

		if( _cachingEnabled() ) {
			_componentDefinitionCache.put( componentName, newComponentDefinition );
		}

		return newComponentDefinition;
	}

	/**
	 * @return The name of the component definition. For class-based component, this will correspond to the component class's simpleName
	 *
	 * FIXME: We still haven't defined what a component's name is. Is it a fully qualified class name? The component's short name? Are there namespaces? Use with care // Hugi 2023-02-09
	 */
	public String name() {
		return _name;
	}

	/**
	 * @return A new component of the given class in the given context
	 *
	 * FIXME: In the case of stateless components, we are going to want to share an already cached instance // Hugi 2022-10-19
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
	 * FIXME: We should try loading the old style HTML-template first. If that succeeds, load the wod file.
	 * Then, if loading of the old style template loading fails, we check for a new style HTML-file instead.
	 * Finally, log a warning if nothing is found (or even hard fail with an exception)
	 */
	private NGElement _loadTemplate() {
		try {
			// Let's try first for the traditional template
			String htmlTemplateString = loadHTMLStringFromTemplateFolder( name() );
			String wodString = loadWODStringFromTemplateFolder( name() );

			// If that fails, let's go for the single file html template
			// FIXME: this is not a good way to check for this. Check for existence of files and determine heuristics from there
			if( htmlTemplateString.isEmpty() && wodString.isEmpty() ) {
				final Optional<byte[]> htmlTemplate = NGResourceLoader.readComponentResource( name() + ".html" );

				if( htmlTemplate.isPresent() ) {
					htmlTemplateString = new String( htmlTemplate.get(), StandardCharsets.UTF_8 );
				}
			}

			// FIXME: I feel like we should be throwing an exception in this case // Hugi 2022-11-27
			if( htmlTemplateString.isEmpty() ) {
				logger.warn( "Component template '%s' not found".formatted( name() ) );
			}

			return NGTemplateParser.parse( htmlTemplateString, wodString, Collections.emptyList() );
		}
		catch( ClassNotFoundException | NGDeclarationFormatException | NGHTMLFormatException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * @return The HTML template for the named component
	 */
	private static String loadHTMLStringFromTemplateFolder( final String templateName ) {
		Objects.requireNonNull( templateName );
		return loadStringFromTemplateFolder( templateName, "html" );
	}

	/**
	 * @return The WOD template for the named component
	 */
	private static String loadWODStringFromTemplateFolder( final String templateName ) {
		Objects.requireNonNull( templateName );
		return loadStringFromTemplateFolder( templateName, "wod" );
	}

	/**
	 * @return The string template for the named component
	 */
	private static String loadStringFromTemplateFolder( final String templateName, final String extension ) {
		Objects.requireNonNull( templateName );
		Objects.requireNonNull( extension );

		final String htmlTemplateFilename = templateName + ".wo/" + templateName + "." + extension;

		final Optional<byte[]> templateBytes = NGResourceLoader.readComponentResource( htmlTemplateFilename );

		if( templateBytes.isEmpty() ) {
			return "";
		}

		return new String( templateBytes.get(), StandardCharsets.UTF_8 );
	}
}