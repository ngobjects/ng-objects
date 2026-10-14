package ng.appserver.templating;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The classpath index behind NGSearchClassPathForElements: what it indexes, and where it looks.
 */
public class TestNGClasspathIndex {

	@Test
	public void indexesTopLevelClassesBySimpleName() {
		final Map<String, List<String>> index = new HashMap<>();
		NGClasspathIndex.add( "a/b/Badge.class", index );
		NGClasspathIndex.add( "c/Badge.class", index );
		NGClasspathIndex.add( "a/b/Badge.class", index ); // the same class twice (a jar and an output folder) is one class
		assertEquals( List.of( "a.b.Badge", "c.Badge" ), index.get( "Badge" ) );
	}

	@Test
	public void skipsWhatATemplateCantName() {
		final Map<String, List<String>> index = new HashMap<>();
		NGClasspathIndex.add( "a/Outer$Inner.class", index );
		NGClasspathIndex.add( "a/Outer$1.class", index );
		NGClasspathIndex.add( "module-info.class", index );
		NGClasspathIndex.add( "a/package-info.class", index );
		NGClasspathIndex.add( "META-INF/versions/21/a/Thing.class", index );
		NGClasspathIndex.add( "a/readme.txt", index );
		assertEquals( Map.of(), index );
	}

	@Test
	public void readsDirectoriesAndJarsAndFollowsManifestClassPath( @TempDir Path temp ) throws IOException {
		final Path classes = temp.resolve( "classes" );
		Files.createDirectories( classes.resolve( "app" ) );
		Files.writeString( classes.resolve( "app/Main.class" ), "" );

		final Path library = temp.resolve( "lib/library.jar" );
		Files.createDirectories( library.getParent() );
		jar( library, null, "lib/Badge.class" );

		// A launcher jar listing the library in its manifest, the way a packaged app's classpath can be
		final Manifest manifest = new Manifest();
		manifest.getMainAttributes().put( Attributes.Name.MANIFEST_VERSION, "1.0" );
		manifest.getMainAttributes().put( Attributes.Name.CLASS_PATH, "lib/library.jar" );
		final Path launcher = temp.resolve( "launcher.jar" );
		jar( launcher, manifest, "launch/Launcher.class" );

		final NGClasspathIndex index = NGClasspathIndex.of( List.of( classes, launcher ) );
		assertEquals( List.of( "app.Main" ), index.classNamesWithSimpleName( "Main" ) );
		assertEquals( List.of( "launch.Launcher" ), index.classNamesWithSimpleName( "Launcher" ) );
		assertEquals( List.of( "lib.Badge" ), index.classNamesWithSimpleName( "Badge" ) );
		assertEquals( List.of(), index.classNamesWithSimpleName( "Missing" ) );
	}

	private static void jar( final Path path, final Manifest manifest, final String... entries ) throws IOException {
		try( JarOutputStream out = manifest != null ? new JarOutputStream( Files.newOutputStream( path ), manifest ) : new JarOutputStream( Files.newOutputStream( path ) )) {
			for( final String entry : entries ) {
				out.putNextEntry( new JarEntry( entry ) );
				out.closeEntry();
			}
		}
	}

	@Test
	public void directoryPackagesAreRecorded( @TempDir Path temp ) throws IOException {
		Files.createDirectories( temp.resolve( "app/components" ) );
		Files.writeString( temp.resolve( "app/components/Main.class" ), "" );
		Files.writeString( temp.resolve( "Root.class" ), "" );
		assertEquals( java.util.Set.of( "app.components", "" ), NGClasspathIndex.of( List.of( temp ) ).directoryPackages() );
	}
}
