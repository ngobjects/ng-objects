package ng.appserver.templating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ng.appserver.templating.NGElementManager.DeclaredElement;
import ng.appserver.templating.definitions.NGElementAPI;
import ng.appserver.templating.elements.NGString;
import ng.appserver.templating.librarytest.one.LibraryTestDeclared;
import ng.appserver.templating.librarytest.one.LibraryTestUndeclared;

/**
 * The element registry: a namespace's elements (the .apiext files in its components folder, published by its taglib or
 * all of them) and its tag aliases, loaded in plugin order;
 * recursive tag resolution; and the classpath search for elements nobody declared.
 */
public class TestNGElementManagerTagAliases {

	/**
	 * A manager with ng-appserver's own namespace loaded, as the core plugin's initialization does
	 */
	private static NGElementManager core() {
		final NGElementManager manager = new NGElementManager( false, false );
		manager.loadNamespace( "ng" );
		return manager;
	}

	private static NGElementAPI api( final String xml ) {
		return NGElementAPI.parse( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ), "test" );
	}

	private static Map<String, DeclaredElement> declared( final NGElementManager manager ) {
		return manager.declaredElements().stream().collect( Collectors.toMap( DeclaredElement::name, e -> e ) );
	}

	@Test
	public void theCoreNamespaceIsLoaded() {
		final NGElementManager manager = core();
		assertEquals( 26, manager.declaredElements().size(), "all framework elements should be declared" );
		assertEquals( "NGCheckbox", manager.resolveTagName( "checkbox" ) );
		assertEquals( "NGString", manager.resolveTagName( "str" ) );
		assertEquals( "AjaxUpdateContainer", manager.resolveTagName( "auc" ) );
	}

	@Test
	public void everyCoreElementHasItsClassAndAPI() {
		for( final DeclaredElement element : core().declaredElements() ) {
			assertNotNull( element.elementClass(), element.name() + " has a class" );
			assertEquals( element.name(), element.elementClass().getSimpleName() );
			assertTrue( element.api().doc() != null && !element.api().doc().isBlank(), element.name() + " is documented" );
		}
	}

	@Test
	public void declaredClassesResolveWithoutAPackageSearch() {
		assertEquals( NGString.class, core().classWithSimpleNameNullIfNotFound( "NGString" ) );
	}

	@Test
	public void tagsForAnElement() {
		assertTrue( core().tagsFor( "NGString" ).contains( "str" ) );
	}

	@Test
	public void elementsAreNamedAfterTheirFiles() {
		final NGElementManager manager = new NGElementManager( false, false );
		manager.loadNamespace( "librarytest" );
		assertEquals( List.of( "LibraryTestDeclared", "LibraryTestTemplate" ), manager.declaredElements().stream().map( DeclaredElement::name ).toList() );
	}

	@Test
	public void aNamespaceIsLoadedByConvention() {
		final NGElementManager manager = new NGElementManager( false, false );
		manager.loadNamespace( "librarytest" );
		final Map<String, DeclaredElement> declared = declared( manager );

		assertEquals( LibraryTestDeclared.class, declared.get( "LibraryTestDeclared" ).elementClass(), "the class comes from the .apiext" );
		assertEquals( "librarytest", declared.get( "LibraryTestDeclared" ).namespace() );
		assertNull( declared.get( "LibraryTestTemplate" ).elementClass(), "a template-only component has no class" );
		assertTrue( declared.get( "LibraryTestTemplate" ).api().wrapsContent() );
		assertEquals( 2, declared.size(), "one element per .apiext in the namespace's components folder" );
		assertTrue( declared.values().stream().allMatch( DeclaredElement::published ), "without a taglib, a namespace publishes every element" );
		assertEquals( "LibraryTestDeclared", manager.resolveTagName( "declared" ) );
		assertEquals( "LibraryTestTemplate", manager.resolveTagName( "template" ) );
	}

	@Test
	public void aTaglibPublishesWhatItLists() {
		final NGElementManager manager = new NGElementManager( false, false );
		manager.loadNamespace( "librarycontract" );
		final Map<String, DeclaredElement> declared = declared( manager );

		assertEquals( 3, declared.size(), declared.keySet().toString() );
		assertTrue( declared.get( "ContractPublished" ).published() );
		assertFalse( declared.get( "ContractInternal" ).published(), "an .apiext the taglib doesn't list is internal" );
		assertTrue( declared.get( "ContractUndocumented" ).published() );
		assertNull( declared.get( "ContractUndocumented" ).api(), "published without an .apiext: undocumented" );
		assertEquals( List.of( "ContractPublished", "ContractUndocumented" ), manager.publishedElements().stream().map( DeclaredElement::name ).toList() );
	}

	@Test
	public void theCoreNamespacePublishesEverything() {
		final NGElementManager manager = core();
		assertEquals( manager.declaredElements(), manager.publishedElements() );
	}

	@Test
	public void aLaterNamespaceOverridesATag() {
		// librarytest maps "str" too; loaded after ng (plugin order), it wins
		final NGElementManager manager = core();
		manager.loadNamespace( "librarytest" );
		assertEquals( "LibraryTestDeclared", manager.resolveTagName( "str" ) );
	}

	@Test
	public void withinANamespaceTheFirstTagStands() {
		final NGElementManager manager = new NGElementManager( false, false );
		manager.registerTagAlias( "badge", "Badge", "app", "first" );
		manager.registerTagAlias( "badge", "OtherBadge", "app", "second" );
		assertEquals( "Badge", manager.resolveTagName( "badge" ) );
	}

	@Test
	public void aMissingClassFailsAtLoadTime() {
		final NGElementManager manager = new NGElementManager( false, false );
		assertThrows( IllegalArgumentException.class, () -> manager.declare( "X", api( "<wodefinitions><wo class=\"does.not.Exist\"/></wodefinitions>" ), "ui", true, "test" ) );
	}

	@Test
	public void aClassThatIsntAnElementIsRefused() {
		final NGElementManager manager = new NGElementManager( false, false );
		assertThrows( IllegalArgumentException.class, () -> manager.declare( "X", api( "<wodefinitions><wo class=\"java.lang.String\"/></wodefinitions>" ), "ui", true, "test" ) );
	}

	@Test
	public void theFirstDeclarationOfAnElementStands() {
		final NGElementManager manager = core();
		manager.declare( "NGString", api( "<wodefinitions><wo class=\"ng.appserver.templating.librarytest.one.LibraryTestDeclared\"/></wodefinitions>" ), "app", true, "test" );
		assertEquals( NGString.class, manager.classWithSimpleNameNullIfNotFound( "NGString" ) );
	}

	@Test
	public void unaliasedNameResolvesToItself() {
		assertEquals( "NGString", core().resolveTagName( "NGString" ) );
		assertEquals( "MyComponent", core().resolveTagName( "MyComponent" ) );
	}

	@Test
	public void resolutionIsRecursive() {
		final NGElementManager manager = core();
		manager.registerTagAlias( "myStr", "str", "app", "test" );
		assertEquals( "NGString", manager.resolveTagName( "myStr" ), "myStr -> str -> NGString" );
	}

	@Test
	public void cyclesDoNotHang() {
		final NGElementManager manager = new NGElementManager( false, false );
		manager.registerTagAlias( "a", "b", "app", "test" );
		manager.registerTagAlias( "b", "a", "app", "test" );
		final String resolved = manager.resolveTagName( "a" );
		assertTrue( resolved.equals( "a" ) || resolved.equals( "b" ) );
	}

	@Test
	public void undeclaredElementsAreFoundOnTheClasspath() {
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

	@Test
	public void aDeclarationIsAuthoritative() {
		// LibraryTestUndeclared is an NGElement class on the classpath; declared without a class, the name has none
		final NGElementManager manager = new NGElementManager( true, false );
		manager.declare( "LibraryTestUndeclared", api( "<wodefinitions><wo content=\"allowed\"/></wodefinitions>" ), "app", true, "test" );
		assertNull( manager.classWithSimpleNameNullIfNotFound( "LibraryTestUndeclared" ) );
	}

	@Test
	public void anElementIsNamedAfterItsClass() {
		final NGElementManager manager = new NGElementManager( false, false );
		final IllegalArgumentException e = assertThrows( IllegalArgumentException.class, () -> manager.declare( "Look", api( "<wodefinitions><wo class=\"ng.appserver.templating.librarytest.one.LibraryTestDeclared\"/></wodefinitions>" ), "app", true, "test" ) );
		assertTrue( e.getMessage().contains( "tag alias" ) );
	}

	@Test
	public void aNamePublishedWithoutAnApiextIsResolvedByName() {
		// A taglib entry with no .apiext says nothing about the class: the element is found on the classpath
		final NGElementManager manager = new NGElementManager( true, false );
		manager.declare( "LibraryTestUndeclared", null, "app", true, "test" );
		assertEquals( LibraryTestUndeclared.class, manager.classWithSimpleNameNullIfNotFound( "LibraryTestUndeclared" ) );
	}
}
