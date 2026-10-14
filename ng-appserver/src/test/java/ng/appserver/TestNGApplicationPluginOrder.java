package ng.appserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ng.plugins.NGPlugin;

/**
 * Plugins are initialized after the plugins they require, whatever order they were found in
 */
public class TestNGApplicationPluginOrder {

	static class Base implements NGPlugin {}

	static class Extension implements NGPlugin {
		@Override
		public List<Class<? extends NGPlugin>> requires() {
			return List.of( Base.class );
		}
	}

	static class Unrelated implements NGPlugin {}

	static class First implements NGPlugin {
		@Override
		public List<Class<? extends NGPlugin>> requires() {
			return List.of( Second.class );
		}
	}

	static class Second implements NGPlugin {
		@Override
		public List<Class<? extends NGPlugin>> requires() {
			return List.of( First.class );
		}
	}

	private static List<Class<?>> classes( final List<NGPlugin> plugins ) {
		return plugins.stream().<Class<?>> map( Object::getClass ).toList();
	}

	@Test
	public void aPluginComesAfterTheOneItRequires() {
		assertEquals( List.of( Base.class, Extension.class ), classes( NGApplication.inRequiredOrder( List.of( new Extension(), new Base() ) ) ) );
	}

	@Test
	public void unrelatedPluginsKeepTheirOrder() {
		assertEquals( List.of( Unrelated.class, Base.class, Extension.class ), classes( NGApplication.inRequiredOrder( List.of( new Unrelated(), new Extension(), new Base() ) ) ) );
	}

	@Test
	public void aMissingRequirementFailsStartup() {
		final IllegalStateException e = assertThrows( IllegalStateException.class, () -> NGApplication.inRequiredOrder( List.of( new Extension() ) ) );
		assertTrue( e.getMessage().contains( "Base" ) );
	}

	@Test
	public void aCycleFailsStartup() {
		final IllegalStateException e = assertThrows( IllegalStateException.class, () -> NGApplication.inRequiredOrder( List.of( new First(), new Second() ) ) );
		assertTrue( e.getMessage().contains( "cycle" ) );
	}
}
