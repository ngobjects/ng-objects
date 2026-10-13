package ng.testapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ng.appserver.NGApplication;
import ng.appserver.properties.NGProperties;
import ng.appserver.properties.StandardDeploymentMode;
import ng.appserver.templating.NGElementManager;
import ng.appserver.templating.NGElementManager.DeclaredElement;
import ng.testapp.components.TALook;

/**
 * The application's own namespace (ng/app: its elements and tag aliases) works as the framework's does: its
 * elements are declared, with their classes and APIs from their .apiext files, a template-only component included, and
 * its tags resolve, to an element it doesn't publish too.
 * The application is created without an adaptor, so nothing listens on a port.
 */
public class TestElementLibrary {

	private static NGElementManager elementManager;

	@BeforeAll
	static void createApplication() throws Exception {
		elementManager = NGApplication.create( Application.class, new NGProperties(), StandardDeploymentMode.Development, true ).elementManager();
	}

	private static Map<String, DeclaredElement> declaredInApp() {
		return elementManager.declaredElements()
				.stream()
				.filter( e -> "app".equals( e.namespace() ) )
				.collect( Collectors.toMap( DeclaredElement::name, e -> e ) );
	}

	@Test
	public void theApplicationsElementsAreDeclared() {
		final Map<String, DeclaredElement> declared = declaredInApp();
		assertEquals( 2, declared.size(), declared.keySet().toString() );
		assertEquals( TALook.class, declared.get( "TALook" ).elementClass() );
		assertNull( declared.get( "TABadge" ).elementClass(), "a template-only component has no class" );
	}

	@Test
	public void itsTagsResolve() {
		assertEquals( "TALook", elementManager.resolveTagName( "look" ) );
		assertEquals( "TAWrapperComponent", elementManager.resolveTagName( "wrapper" ), "an element the application doesn't publish" );
		assertEquals( "TABadge", elementManager.resolveTagName( "badge" ) );
		assertEquals( "NGString", elementManager.resolveTagName( "WOString" ), "the compatibility alias" );
	}

	@Test
	public void theFrameworksElementsAreStillThere() {
		assertEquals( "NGString", elementManager.resolveTagName( "str" ) );
		assertTrue( elementManager.declaredElements().stream().anyMatch( e -> "ng".equals( e.namespace() ) ) );
	}

	@Test
	public void eachElementsAPIComesFromItsApiext() {
		final Map<String, DeclaredElement> declared = declaredInApp();
		assertTrue( declared.get( "TALook" ).api().doc().startsWith( "The test application's page frame" ) );
		assertTrue( declared.get( "TABadge" ).api().wrapsContent() );
	}

	@Test
	public void anElementsTags() {
		assertEquals( java.util.List.of( "look" ), elementManager.tagsFor( "TALook" ) );
	}
}
