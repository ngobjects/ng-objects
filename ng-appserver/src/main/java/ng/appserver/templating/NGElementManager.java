package ng.appserver.templating;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGContext;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.definitions.NGElementAPI;
import ng.appserver.templating.definitions.NGTaglib;
import ng.appserver.templating.elements.NGComponentReference;

/**
 * Manages access to and location of dynamic elements and components registered with a project
 */

public class NGElementManager {

	private static final Logger logger = LoggerFactory.getLogger( NGElementManager.class );

	/**
	 * To ease the porting of older templates to this system we allow unnamespaced elements. This is done
	 * by designating the namespace "wo" a "magic namespace" which when used for locating elements means "search every namespace".
	 *
	 * This is probably/hopefully temporary.
	 */
	public static final String GLOBAL_UNNAMESPACED_NAMESPACE = "wo";

	/**
	 * A namespace's tag aliases, in its resource folder ({@code ng/<namespace>/tag-aliases.properties}): one line per
	 * tag, {@code tagName = element}, where the element is an element's name (or another tag).
	 */
	public static final String TAG_ALIASES_FILENAME = "tag-aliases.properties";

	/**
	 * The elements the namespaces declare, by element name, in the order they were loaded: each {@code .apiext} in a
	 * namespace's components folder, and each name its taglib publishes. An element nobody declares can still render
	 * (see {@code NGSearchClassPathForElements}).
	 */
	private final Map<String, DeclaredElement> _declaredElements = new LinkedHashMap<>();

	/**
	 * The classes of declared elements, by element name
	 */
	private final Map<String, Class<?>> _elementClasses = new ConcurrentHashMap<>();

	/**
	 * Tag names mapped to element names (or to other tags; resolution is recursive), e.g. {@code str -> NGString},
	 * from the namespaces' tag alias files, with the namespace that declared each. Namespaces are loaded in plugin
	 * order, and a later one's tag overrides an earlier one's: a plugin can rename the tags of one it builds on, and the
	 * application, loaded last, has the last word.
	 */
	private final Map<String, TagAlias> _tagAliases = new ConcurrentHashMap<>();

	/**
	 * Whether an element that isn't declared is searched for on the classpath, by simple name (the
	 * {@code NGSearchClassPathForElements} property). On by default, like WO, and the same in every deployment
	 * mode, so an app that renders in development renders in production.
	 */
	private final boolean _searchClassPathForElements;

	/**
	 * Whether an element found only on the classpath is logged (once per element). Set in development, so
	 * undeclared elements are visible.
	 */
	private final boolean _warnAboutUndeclaredElements;

	/**
	 * The classpath's classes by simple name, built the first time an element is searched for there
	 */
	private NGClasspathIndex _classpathIndex;

	/**
	 * Elements already reported as found only on the classpath
	 */
	private final Set<String> _reportedUndeclaredElements = ConcurrentHashMap.newKeySet();

	/**
	 * @param searchClassPathForElements Whether an element that isn't declared is
	 * searched for on the classpath ({@code NGSearchClassPathForElements})
	 * @param warnAboutUndeclaredElements Whether elements found only on the classpath are logged, once each (development)
	 */
	public NGElementManager( final boolean searchClassPathForElements, final boolean warnAboutUndeclaredElements ) {
		_searchClassPathForElements = searchClassPathForElements;
		_warnAboutUndeclaredElements = warnAboutUndeclaredElements;
	}

	/**
	 * @return Every declared element, published or internal, in the order loaded
	 */
	public List<DeclaredElement> declaredElements() {
		synchronized( _declaredElements ) {
			return List.copyOf( _declaredElements.values() );
		}
	}

	/**
	 * @return The elements the namespaces publish, in the order loaded: what documentation and tooling offer
	 */
	public List<DeclaredElement> publishedElements() {
		return declaredElements().stream().filter( DeclaredElement::published ).toList();
	}

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
	 * @return The class of the element with the given name: a declared element's, or (if enabled) one found on the
	 * classpath by simple name. Null if there's none, which for a template-only component is the expected answer.
	 * An .apiext is authoritative: an element whose .apiext names no class (a template-only component) has none,
	 * whatever the classpath holds. A name a taglib publishes without an .apiext says nothing about its class, so it's
	 * resolved by name like any other.
	 */
	public Class classWithSimpleNameNullIfNotFound( String simpleClassName ) {

		Objects.requireNonNull( simpleClassName );

		final Class<?> elementClass = _elementClasses.get( simpleClassName );

		if( elementClass != null ) {
			return elementClass;
		}

		synchronized( _declaredElements ) {
			final DeclaredElement declared = _declaredElements.get( simpleClassName );

			if( declared != null && declared.api() != null ) {
				return null;
			}
		}

		if( _searchClassPathForElements ) {
			return elementClassOnClasspath( simpleClassName );
		}

		return null;
	}

	/**
	 * @return The element class with the given simple name found anywhere on the classpath, or null if there's none.
	 * @throws NGElementNotFoundException if several element classes have the name: which one a template means can't be guessed
	 */
	private Class<?> elementClassOnClasspath( final String simpleClassName ) {
		final List<Class<?>> candidates = new ArrayList<>();

		for( final String className : classpathIndex().classNamesWithSimpleName( simpleClassName ) ) {
			addElementClass( className, candidates );
		}

		// Not in the index: a class created after it was built (during development) is in one of the packages already
		// seen in the classpath's directories, the application's own output folders
		if( candidates.isEmpty() ) {
			for( final String packageName : classpathIndex().directoryPackages() ) {
				addElementClass( packageName.isEmpty() ? simpleClassName : packageName + "." + simpleClassName, candidates );
			}
		}

		if( candidates.isEmpty() ) {
			return null;
		}

		if( candidates.size() > 1 ) {
			throw new NGElementNotFoundException( "Several element classes are named '%s' (%s); declare the one you mean (an .apiext in its namespace's components folder)".formatted( simpleClassName, candidates.stream().map( Class::getName ).toList() ), simpleClassName );
		}

		final Class<?> found = candidates.get( 0 );

		if( _warnAboutUndeclaredElements && _reportedUndeclaredElements.add( simpleClassName ) ) {
			logger.warn( "'{}' isn't declared; found on the classpath as {}. Add ng/<namespace>/components/{}.apiext to declare and document it.", simpleClassName, found.getName(), simpleClassName );
		}

		return found;
	}

	/**
	 * Adds the named class to the candidates if it exists and is an element
	 */
	private static void addElementClass( final String className, final List<Class<?>> candidates ) {
		try {
			// Not initialized: a candidate that isn't an element must not run its static initializers
			final Class<?> candidate = Class.forName( className, false, NGElementManager.class.getClassLoader() );

			if( NGElement.class.isAssignableFrom( candidate ) ) {
				candidates.add( candidate );
			}
		}
		catch( ClassNotFoundException e ) {
			// Not a class there: the usual answer when probing a package
		}
		catch( LinkageError e ) {
			// A class that exists but can't be loaded (a missing dependency, a broken hot swap) is the real cause of whatever
			// fails next, so it mustn't vanish
			logger.warn( "The class {} exists but can't be loaded, so it isn't an element candidate", className, e );
		}
	}

	/**
	 * Uses the given index instead of building one from the classpath (for tests)
	 */
	synchronized void useClasspathIndex( final NGClasspathIndex index ) {
		_classpathIndex = index;
	}

	private synchronized NGClasspathIndex classpathIndex() {
		if( _classpathIndex == null ) {
			_classpathIndex = NGClasspathIndex.ofClasspath();
		}

		return _classpathIndex;
	}

	/**
	 * @return Every tag mapped to its target
	 *
	 * FIXME: Definitely not the final home of this functionality // Hugi 2022-04-23
	 */
	public Map<String, String> elementTagNames() {
		final Map<String, String> tags = new HashMap<>();
		_tagAliases.forEach( ( tag, alias ) -> tags.put( tag, alias.target() ) );
		return tags;
	}

	/**
	 * @return The tags that resolve to the named element, sorted
	 */
	public List<String> tagsFor( final String elementName ) {
		return _tagAliases.keySet()
				.stream()
				.filter( tag -> !tag.equals( elementName ) && resolveTagName( tag ).equals( elementName ) )
				.sorted()
				.toList();
	}

	/**
	 * @return What the tag maps to directly, or null if it maps to nothing
	 */
	private String tagTarget( final String tagName ) {
		final TagAlias alias = _tagAliases.get( tagName );
		return alias != null ? alias.target() : null;
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

		while( (next = tagTarget( current )) != null ) {
			if( !seen.add( next ) ) {
				logger.warn( "Cycle detected resolving tag alias for '{}' (at '{}'); stopping.", elementIdentifier, next );
				break;
			}
			current = next;
		}

		return current;
	}

	/**
	 * Loads a namespace's elements and tags, as each plugin is initialized, in plugin order. The elements are in its
	 * components folder ({@code ng/<namespace>/components}; several jars can contribute to one namespace): each
	 * {@code .apiext} is an element, named after the file. Which of them the namespace publishes is its contract:
	 *
	 * - With a {@value NGTaglib#FILENAME} in the components folder, the elements it lists; the rest are internal. A listed
	 *   element needn't have an {@code .apiext} (it's then undocumented, resolved by name).
	 * - Without one, every element.
	 *
	 * Then the namespace's tags, from {@code ng/<namespace>/tag-aliases.properties}.
	 */
	public void loadNamespace( final String namespace ) {
		Objects.requireNonNull( namespace );

		final String componentsFolder = "ng/%s/components".formatted( namespace );
		final Set<String> published = publishedNames( componentsFolder );

		// The folder is listed through its directory entry, which a jar written without directory entries lacks: its
		// taglib is still found (by its own path), so say why its elements are missing instead of declaring nothing
		if( published != null && resources( componentsFolder ).isEmpty() ) {
			logger.warn( "Namespace '{}' has a {} but its components folder can't be listed (a jar without directory entries?): only the elements it publishes are declared, without their .apiext", namespace, NGTaglib.FILENAME );
		}
		final Set<String> declared = new HashSet<>();

		for( final URL url : filesIn( componentsFolder, ".apiext" ) ) {
			final String filename = url.getPath().substring( url.getPath().lastIndexOf( '/' ) + 1 );
			final String name = filename.substring( 0, filename.length() - ".apiext".length() );

			try( InputStream in = url.openStream()) {
				declare( name, NGElementAPI.parse( in, url.toString() ), namespace, published == null || published.contains( name ), url.toString() );
				declared.add( name );
			}
			catch( final IOException e ) {
				logger.warn( "Failed to read {}", url, e );
			}
		}

		if( published != null ) {
			for( final String name : published ) {
				if( !declared.contains( name ) ) {
					declare( name, null, namespace, true, componentsFolder + "/" + NGTaglib.FILENAME );
				}
			}
		}

		for( final URL url : resources( "ng/%s/%s".formatted( namespace, TAG_ALIASES_FILENAME ) ) ) {
			try( InputStream in = url.openStream()) {
				final Properties properties = new Properties();
				properties.load( in );

				for( final String tag : properties.stringPropertyNames() ) {
					registerTagAlias( tag.strip(), properties.getProperty( tag ).strip(), namespace, url.toString() );
				}
			}
			catch( final IOException e ) {
				logger.warn( "Failed to read the tag aliases {}", url, e );
			}
		}
	}

	/**
	 * @return The names the taglib in the components folder publishes, or null when there's none (the namespace then
	 * publishes every element). Several jars' taglibs for one namespace add up.
	 */
	private static Set<String> publishedNames( final String componentsFolder ) {
		Set<String> names = null;

		for( final URL url : resources( componentsFolder + "/" + NGTaglib.FILENAME ) ) {
			try( InputStream in = url.openStream()) {
				if( names == null ) {
					names = new LinkedHashSet<>();
				}

				names.addAll( NGTaglib.parse( in, url.toString() ).elementNames() );
			}
			catch( final IOException e ) {
				logger.warn( "Failed to read the taglib {}", url, e );
			}
		}

		return names;
	}

	/**
	 * Declares an element, with its API (null for a published element without an {@code .apiext}). One with a class
	 * gets that class (fully qualified, loaded here, so a wrong name fails at startup, not at render); one without is
	 * resolved by name, as a template-only component is.
	 *
	 * When two elements have the same name, the first stands and the other is ignored with a warning.
	 */
	void declare( final String name, final NGElementAPI api, final String namespace, final boolean published, final String source ) {
		synchronized( _declaredElements ) {
			final DeclaredElement existing = _declaredElements.get( name );

			// Checked before the class is loaded: an ignored declaration's class doesn't matter
			if( existing != null ) {
				logger.warn( "Ignoring the element '{}' in {}; namespace '{}' already has one", name, source, existing.namespace() );
				return;
			}
		}

		final Class<? extends NGElement> elementClass = api != null && api.className() != null ? declaredClass( name, api.className(), source ) : null;

		synchronized( _declaredElements ) {
			final DeclaredElement existing = _declaredElements.putIfAbsent( name, new DeclaredElement( name, namespace, elementClass, api, published ) );

			if( existing != null ) {
				logger.warn( "Ignoring the element '{}' in {}; namespace '{}' already has one", name, source, existing.namespace() );
				return;
			}
		}

		if( elementClass != null ) {
			_elementClasses.put( name, elementClass );
		}
	}

	/**
	 * @return The declared class, which must exist, be an element, and have the element's name as its simple name: an
	 * element has one name (the file's), and tag aliases are what give it others
	 */
	@SuppressWarnings("unchecked")
	private static Class<? extends NGElement> declaredClass( final String name, final String className, final String source ) {
		if( !className.substring( className.lastIndexOf( '.' ) + 1 ).equals( name ) ) {
			throw new IllegalArgumentException( "%s: the element '%s' names the class '%s'; an element's file is named after its class (use a tag alias for another name)".formatted( source, name, className ) );
		}

		final Class<?> elementClass;

		try {
			elementClass = Class.forName( className, false, NGElementManager.class.getClassLoader() );
		}
		catch( ClassNotFoundException e ) {
			throw new IllegalArgumentException( "%s: the class '%s' of element '%s' doesn't exist".formatted( source, className, name ), e );
		}

		if( !NGElement.class.isAssignableFrom( elementClass ) ) {
			throw new IllegalArgumentException( "%s: the class '%s' of element '%s' isn't an NGElement".formatted( source, className, name ) );
		}

		return (Class<? extends NGElement>)elementClass;
	}

	/**
	 * Maps a tag to its target for the namespace. A later namespace's tag overrides an earlier one's (plugin order); two
	 * files of one namespace that disagree keep the first, with a warning (classpath order is unspecified).
	 */
	void registerTagAlias( final String tag, final String target, final String namespace, final String source ) {
		if( tag.isEmpty() || target.isEmpty() ) {
			return;
		}

		final TagAlias existing = _tagAliases.get( tag );

		if( existing != null && !existing.target().equals( target ) ) {
			if( existing.namespace().equals( namespace ) ) {
				logger.warn( "Ignoring the tag '{}' -> '{}' in {}; namespace '{}' already maps it to '{}'", tag, target, source, namespace, existing.target() );
				return;
			}

			logger.info( "The tag '{}' -> '{}' of namespace '{}' overrides namespace '{}''s '{}'", tag, target, namespace, existing.namespace(), existing.target() );
		}

		_tagAliases.put( tag, new TagAlias( target, namespace ) );
	}

	/**
	 * @return The files with the extension directly in the folder, in every directory and jar on the classpath that has
	 * it, sorted by name within each
	 */
	private static List<URL> filesIn( final String folder, final String extension ) {
		final List<URL> files = new ArrayList<>();

		for( final URL folderURL : resources( folder ) ) {
			try {
				if( "file".equals( folderURL.getProtocol() ) ) {
					try( var paths = Files.list( Path.of( folderURL.toURI() ) )) {
						for( final Path path : paths.filter( p -> p.toString().endsWith( extension ) ).sorted().toList() ) {
							files.add( path.toUri().toURL() );
						}
					}
				}
				else if( folderURL.openConnection() instanceof JarURLConnection connection ) {
					connection.setUseCaches( false );

					try( JarFile jar = connection.getJarFile()) {
						final String prefix = folder + "/";
						final List<String> names = jar.stream()
								.map( JarEntry::getName )
								.filter( name -> name.startsWith( prefix ) && name.indexOf( '/', prefix.length() ) < 0 && name.endsWith( extension ) )
								.sorted()
								.toList();

						for( final String name : names ) {
							files.add( URI.create( "jar:" + connection.getJarFileURL() + "!/" + name ).toURL() );
						}
					}
				}
				else {
					logger.warn( "Can't list {}: the {} protocol isn't supported, so the elements there aren't declared", folderURL, folderURL.getProtocol() );
				}
			}
			catch( final IOException | URISyntaxException e ) {
				logger.warn( "Failed to list {}", folderURL, e );
			}
		}

		return files;
	}

	/**
	 * @return Every resource at the path on the classpath (several jars can have one)
	 */
	private static List<URL> resources( final String path ) {
		try {
			return Collections.list( NGElementManager.class.getClassLoader().getResources( path ) );
		}
		catch( final IOException e ) {
			logger.warn( "Failed to look for {} on the classpath", path, e );
			return List.of();
		}
	}

	/**
	 * A tag's target, and the namespace that declared it
	 */
	private record TagAlias( String target, String namespace ) {}

	/**
	 * A declared element: its namespace, its class (null for a template-only component), its API from its {@code .apiext}
	 * (null for a published element without one), and whether its namespace publishes it
	 */
	public record DeclaredElement( String name, String namespace, Class<? extends NGElement> elementClass, NGElementAPI api, boolean published ) {}
}
