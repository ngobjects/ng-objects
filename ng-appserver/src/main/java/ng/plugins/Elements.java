package ng.plugins;

import ng.appserver.templating.NGElement;

/**
 * Registered a plugin's elements in code. Ignored: a namespace's elements are declared by the .apiext files in its
 * components folder, their tags by its tag-aliases.properties, and an element nobody declares is found on the
 * classpath by name. Kept so plugins that still return one compile while they're cleaned out.
 *
 * FIXME: Delete before 0.1.6 is released, with NGPlugin.elements() // Hugi 2026-10-14
 */
@Deprecated
public class Elements {

	private Elements() {}

	public static Elements create() {
		return new Elements();
	}

	/**
	 * Ignored. The element is found on the classpath by name; its tags belong in the namespace's tag-aliases.properties.
	 */
	public Elements elementClass( Class<? extends NGElement> elementClass, String... tagNames ) {
		return this;
	}

	/**
	 * Ignored. The package's elements are found on the classpath by name.
	 */
	public Elements elementPackage( String packageName ) {
		return this;
	}

	/**
	 * Ignored. Tags belong in the namespace's tag-aliases.properties.
	 */
	public Elements elementAlias( String tagName, String... tagAliases ) {
		return this;
	}
}
