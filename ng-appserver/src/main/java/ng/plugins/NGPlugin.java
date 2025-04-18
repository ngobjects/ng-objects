package ng.plugins;

import ng.appserver.NGApplication;

/**
 * Defines an extension point/module to be used with the framework
 */

public interface NGPlugin {

	/**
	 * Executed at application startup to initialize the plugin.
	 */
	public default void load( NGApplication application ) {}

	/**
	 * The module's namespace. Defaults to the class namespace.
	 * The given namespace will be used for locating resources and elements defined by this module.
	 */
	public default String namespace() {
		return getClass().getSimpleName();
	}

	public default Elements elements() {
		return Elements.create();
	}
}