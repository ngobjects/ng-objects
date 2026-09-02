package ng.appserver.templating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The tag registry is declared in parsley-tag-aliases.properties and loaded by NGElementManager;
 * these tests pin the loading, the recursive resolution and the cycle guard.
 */
public class TestNGElementManagerTagAliases {

	@Test
	public void shippedRegistryIsLoadedFromTheClasspath() {
		final NGElementManager manager = new NGElementManager();

		// The one WO/NG spelling divergence that used to trip the editor — and a few plain ones.
		assertEquals( "NGCheckbox", manager.resolveTagName( "checkbox" ) );
		assertEquals( "NGString", manager.resolveTagName( "str" ) );
		assertEquals( "NGConditional", manager.resolveTagName( "if" ) );
		assertEquals( "AjaxUpdateContainer", manager.resolveTagName( "auc" ) );
		assertTrue( manager.elementTagNames().size() >= 26, "all framework tags should be declared" );
	}

	@Test
	public void unaliasedNameResolvesToItself() {
		assertEquals( "NGString", new NGElementManager().resolveTagName( "NGString" ) );
		assertEquals( "MyComponent", new NGElementManager().resolveTagName( "MyComponent" ) );
	}

	@Test
	public void resolutionIsRecursive() {
		final NGElementManager manager = new NGElementManager();
		manager.registerTagAlias( "myStr", "str" );
		assertEquals( "NGString", manager.resolveTagName( "myStr" ) );
	}

	@Test
	public void codeRegistrationOverridesADeclaredAlias() {
		final NGElementManager manager = new NGElementManager();
		manager.registerTagAlias( "str", "MyOwnString" );
		assertEquals( "MyOwnString", manager.resolveTagName( "str" ) );
	}

	@Test
	public void cyclesDoNotHang() {
		final NGElementManager manager = new NGElementManager();
		manager.registerTagAlias( "a", "b" );
		manager.registerTagAlias( "b", "a" );
		final String resolved = manager.resolveTagName( "a" );
		assertTrue( resolved.equals( "a" ) || resolved.equals( "b" ) );
	}
}
