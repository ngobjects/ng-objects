package ng.appserver.templating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ng.appserver.templating.librarytest.one.LibraryTestUndeclared;

/**
 * The tag registry is declared in ng-tag-aliases.properties and loaded by NGElementManager;
 * these tests pin the loading, the recursive resolution and the cycle guard.
 */
public class TestNGElementManagerTagAliases {

	@Test
	public void shippedRegistryIsLoadedFromTheClasspath() {
		final NGElementManager manager = new NGElementManager( false, false );

		// The one WO/NG spelling divergence that used to trip the editor — and a few plain ones.
		assertEquals( "NGCheckbox", manager.resolveTagName( "checkbox" ) );
		assertEquals( "NGString", manager.resolveTagName( "str" ) );
		assertEquals( "NGConditional", manager.resolveTagName( "if" ) );
		assertEquals( "AjaxUpdateContainer", manager.resolveTagName( "auc" ) );
		assertTrue( manager.elementTagNames().size() >= 26, "all framework tags should be declared" );
	}

	@Test
	public void unaliasedNameResolvesToItself() {
		assertEquals( "NGString", new NGElementManager( false, false ).resolveTagName( "NGString" ) );
		assertEquals( "MyComponent", new NGElementManager( false, false ).resolveTagName( "MyComponent" ) );
	}

	@Test
	public void resolutionIsRecursive() {
		final NGElementManager manager = new NGElementManager( false, false );
		manager.registerTagAlias( "myStr", "str" );
		assertEquals( "NGString", manager.resolveTagName( "myStr" ) );
	}

	@Test
	public void codeRegistrationOverridesADeclaredAlias() {
		final NGElementManager manager = new NGElementManager( false, false );
		manager.registerTagAlias( "str", "MyOwnString" );
		assertEquals( "MyOwnString", manager.resolveTagName( "str" ) );
	}

	@Test
	public void cyclesDoNotHang() {
		final NGElementManager manager = new NGElementManager( false, false );
		manager.registerTagAlias( "a", "b" );
		manager.registerTagAlias( "b", "a" );
		final String resolved = manager.resolveTagName( "a" );
		assertTrue( resolved.equals( "a" ) || resolved.equals( "b" ) );
	}

	@Test
	public void unregisteredElementsAreFoundOnTheClasspath() {
		assertEquals( LibraryTestUndeclared.class, new NGElementManager( true, false ).classWithSimpleNameNullIfNotFound( "LibraryTestUndeclared" ) );
	}

	@Test
	public void theClasspathSearchCanBeSwitchedOff() {
		assertNull( new NGElementManager( false, false ).classWithSimpleNameNullIfNotFound( "LibraryTestUndeclared" ) );
	}

	@Test
	public void classesThatArentElementsArentFound() {
		assertNull( new NGElementManager( true, false ).classWithSimpleNameNullIfNotFound( "TestNGElementManagerTagAliases" ) );
	}

	@Test
	public void anAmbiguousNameIsRefusedNotGuessed() {
		final NGElementNotFoundException e = assertThrows( NGElementNotFoundException.class, () -> new NGElementManager( true, false ).classWithSimpleNameNullIfNotFound( "LibraryTestDuplicate" ) );
		assertTrue( e.getMessage().contains( "librarytest.one.LibraryTestDuplicate" ) && e.getMessage().contains( "librarytest.two.LibraryTestDuplicate" ) );
	}

	@Test
	public void aClassCreatedAfterTheIndexIsFoundInADirectoryPackage( @TempDir Path temp ) throws IOException {
		// An index that has seen the package (in a directory) but not the class, as when the class was created after it was built
		final Path folder = temp.resolve( "ng/appserver/templating/librarytest/one" );
		Files.createDirectories( folder );
		Files.writeString( folder.resolve( "SomethingElse.class" ), "" );

		final NGElementManager manager = new NGElementManager( true, false );
		manager.useClasspathIndex( NGClasspathIndex.of( List.of( temp ) ) );
		assertEquals( LibraryTestUndeclared.class, manager.classWithSimpleNameNullIfNotFound( "LibraryTestUndeclared" ) );
	}
}
