package ng.appserver.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ng.appserver.resources.NGResourceLoader.JavaClasspathResourceSource;

public class TestNGResourceLoader {

	private static final JavaClasspathResourceSource SOURCE = new JavaClasspathResourceSource( "test-resource-source" );

	@Test
	public void aFileIsAResource() {
		assertEquals( "contents", new String( SOURCE.resourceWithPath( "dir/file.txt" ).get().bytes(), StandardCharsets.UTF_8 ) );
	}

	@Test
	public void aDirectoryIsntAResource() {
		assertTrue( SOURCE.resourceWithPath( "dir" ).isEmpty() );
		assertTrue( SOURCE.resourceWithPath( "dir/" ).isEmpty() );
	}

	@Test
	public void aDirectoryInAJarIsntAResource( @TempDir final Path directory ) throws IOException {
		final Path jar = directory.resolve( "test.jar" );

		try( final JarOutputStream out = new JarOutputStream( Files.newOutputStream( jar ) )) {
			out.putNextEntry( new JarEntry( "dir/" ) );
			out.closeEntry();
			out.putNextEntry( new JarEntry( "dir/file.txt" ) );
			out.write( "contents".getBytes( StandardCharsets.UTF_8 ) );
			out.closeEntry();
		}

		assertTrue( JavaClasspathResourceSource.isDirectory( URI.create( "jar:" + jar.toUri() + "!/dir/" ).toURL() ) );
		assertFalse( JavaClasspathResourceSource.isDirectory( URI.create( "jar:" + jar.toUri() + "!/dir/file.txt" ).toURL() ) );
	}

	@Test
	public void aFileSystemDirectory( @TempDir final Path directory ) throws IOException {
		final Path file = Files.writeString( directory.resolve( "file.txt" ), "contents" );
		final URL directoryURL = directory.toUri().toURL();
		assertTrue( JavaClasspathResourceSource.isDirectory( directoryURL ) );
		assertFalse( JavaClasspathResourceSource.isDirectory( file.toUri().toURL() ) );
	}
}
