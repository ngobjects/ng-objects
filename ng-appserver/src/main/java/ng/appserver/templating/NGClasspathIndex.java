package ng.appserver.templating;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The top-level classes on the classpath by simple name, for finding an element that isn't declared (see
 * {@code NGSearchClassPathForElements}) — the way WO finds elements: by name, wherever they are.
 *
 * Only names are indexed; nothing is loaded here. The caller loads the few candidates matching a requested name.
 * Directories and jars on {@code java.class.path} are read, and a jar's manifest {@code Class-Path} is followed
 * (a launcher jar that lists the real classpath in its manifest).
 */

public class NGClasspathIndex {

	private static final Logger logger = LoggerFactory.getLogger( NGClasspathIndex.class );

	/**
	 * Simple class name -> fully qualified names of the classes with that simple name
	 */
	private final Map<String, List<String>> _classNamesBySimpleName;

	/**
	 * The packages of the classes in the classpath's directories: an application's own output folders, where classes
	 * appear during development after the index was built
	 */
	private final Set<String> _directoryPackages;

	private NGClasspathIndex( final Map<String, List<String>> classNamesBySimpleName, final Set<String> directoryPackages ) {
		_classNamesBySimpleName = classNamesBySimpleName;
		_directoryPackages = directoryPackages;
	}

	/**
	 * @return An index of the current JVM's classpath
	 */
	public static NGClasspathIndex ofClasspath() {
		final List<Path> entries = new ArrayList<>();

		for( final String entry : System.getProperty( "java.class.path", "" ).split( File.pathSeparator ) ) {
			if( !entry.isBlank() ) {
				entries.add( Path.of( entry ) );
			}
		}

		final long start = System.currentTimeMillis();
		final NGClasspathIndex index = of( entries );
		logger.debug( "Indexed {} class names from {} classpath entries in {} ms", index._classNamesBySimpleName.size(), entries.size(), System.currentTimeMillis() - start );
		return index;
	}

	/**
	 * @return An index of the given classpath entries (directories and jars)
	 */
	public static NGClasspathIndex of( final List<Path> entries ) {
		final Map<String, List<String>> index = new HashMap<>();
		final Set<String> directoryPackages = new TreeSet<>();
		final Set<Path> seen = new HashSet<>();
		final Deque<Path> queue = new ArrayDeque<>( entries );

		while( !queue.isEmpty() ) {
			final Path entry = queue.removeFirst().toAbsolutePath().normalize();

			if( !seen.add( entry ) ) {
				continue;
			}

			try {
				if( Files.isDirectory( entry ) ) {
					indexDirectory( entry, index, directoryPackages );
				}
				else if( Files.isRegularFile( entry ) && entry.toString().endsWith( ".jar" ) ) {
					queue.addAll( indexJar( entry, index ) );
				}
			}
			catch( IOException e ) {
				logger.warn( "Couldn't index classpath entry {}", entry, e );
			}
		}

		return new NGClasspathIndex( index, directoryPackages );
	}

	/**
	 * @return The fully qualified names of the top-level classes with the given simple name (empty if there are none)
	 */
	public List<String> classNamesWithSimpleName( final String simpleName ) {
		return _classNamesBySimpleName.getOrDefault( simpleName, List.of() );
	}

	/**
	 * @return The packages of the classes in the classpath's directories, the default package as an empty string
	 */
	public Set<String> directoryPackages() {
		return _directoryPackages;
	}

	private static void indexDirectory( final Path directory, final Map<String, List<String>> index, final Set<String> directoryPackages ) throws IOException {
		try( Stream<Path> files = Files.walk( directory )) {
			files.filter( f -> f.toString().endsWith( ".class" ) ).forEach( f -> {
				final String path = directory.relativize( f ).toString().replace( File.separatorChar, '/' );
				add( path, index );

				if( !path.startsWith( "META-INF/" ) ) {
					directoryPackages.add( path.contains( "/" ) ? path.substring( 0, path.lastIndexOf( '/' ) ).replace( '/', '.' ) : "" );
				}
			} );
		}
	}

	/**
	 * Indexes the jar's classes
	 *
	 * @return The jars its manifest's {@code Class-Path} names, for indexing too
	 */
	private static List<Path> indexJar( final Path jar, final Map<String, List<String>> index ) throws IOException {
		final List<Path> referenced = new ArrayList<>();

		try( JarFile jarFile = new JarFile( jar.toFile() )) {
			jarFile.stream().forEach( e -> add( e.getName(), index ) );

			final Manifest manifest = jarFile.getManifest();
			final String classPath = manifest != null ? manifest.getMainAttributes().getValue( Attributes.Name.CLASS_PATH ) : null;

			if( classPath != null ) {
				for( final String reference : classPath.trim().split( "\\s+" ) ) {
					if( !reference.isEmpty() ) {
						referenced.add( jar.resolveSibling( reference.replace( "%20", " " ) ) );
					}
				}
			}
		}

		return referenced;
	}

	/**
	 * Adds a class file's path ({@code a/b/C.class}) to the index under its simple name. Nested and anonymous
	 * classes ({@code C$Inner}), {@code module-info} and {@code package-info} are skipped: none is an element a template can name.
	 */
	static void add( final String path, final Map<String, List<String>> index ) {
		if( !path.endsWith( ".class" ) || path.startsWith( "META-INF/" ) ) {
			return;
		}

		final String className = path.substring( 0, path.length() - ".class".length() ).replace( '/', '.' );
		final String simpleName = className.substring( className.lastIndexOf( '.' ) + 1 );

		if( simpleName.contains( "$" ) || simpleName.equals( "module-info" ) || simpleName.equals( "package-info" ) ) {
			return;
		}

		final List<String> names = index.computeIfAbsent( simpleName, _unused -> new ArrayList<>( 1 ) );

		// The same class can be on the classpath twice (a jar and an output folder); it's still one class
		if( !names.contains( className ) ) {
			names.add( className );
		}
	}
}
