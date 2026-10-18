package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestNGApplicationOutputPath {

	@TempDir
	private Path _dir;

	private PrintStream _out;
	private PrintStream _err;

	@BeforeEach
	public void rememberStreams() {
		_out = System.out;
		_err = System.err;
	}

	@AfterEach
	public void restoreStreams() throws IOException {
		if( System.out != _out ) {
			System.out.close();
		}

		System.setOut( _out );
		System.setErr( _err );
		Files.setPosixFilePermissions( _dir, PosixFilePermissions.fromString( "rwx------" ) );
	}

	@Test
	public void redirectsAndArchivesThePreviousLog() throws IOException {
		final Path log = _dir.resolve( "App-1" );
		Files.writeString( log, "previous run" );

		NGApplication.redirectOutputToFilesIfOutputPathSet( log.toString() );

		assertNotSame( _out, System.out );
		assertTrue( Files.exists( log ) );

		final List<Path> archived;

		try( var files = Files.list( _dir )) {
			archived = files.filter( p -> p.getFileName().toString().startsWith( "App-1." ) ).toList();
		}

		assertEquals( 1, archived.size() );
		assertEquals( "previous run", Files.readString( archived.get( 0 ) ) );
	}

	@Test
	public void carriesOnWhenTheFileCantBeOpened() throws IOException {
		makeReadOnly( _dir );

		NGApplication.redirectOutputToFilesIfOutputPathSet( _dir.resolve( "App-1" ).toString() );

		assertSame( _out, System.out );
		assertSame( _err, System.err );
	}

	@Test
	public void keepsThePreviousLogWhenItCantBeRenamed() throws IOException {
		final Path log = _dir.resolve( "App-1" );
		Files.writeString( log, "previous run" );
		makeReadOnly( _dir );

		NGApplication.redirectOutputToFilesIfOutputPathSet( log.toString() );

		assertSame( _out, System.out );
		assertEquals( "previous run", Files.readString( log ) );
	}

	@Test
	public void carriesOnWhenThePathIsADirectory() throws IOException {
		final Path directory = Files.createDirectory( _dir.resolve( "App-1" ) );

		NGApplication.redirectOutputToFilesIfOutputPathSet( directory.toString() );

		assertSame( _out, System.out );
		assertTrue( Files.isDirectory( directory ) );
	}

	private static void makeReadOnly( final Path directory ) throws IOException {
		Files.setPosixFilePermissions( directory, PosixFilePermissions.fromString( "r-x------" ) );
		assumeFalse( Files.isWritable( directory ), "Permissions aren't enforced for this user (root?)" );
	}
}
