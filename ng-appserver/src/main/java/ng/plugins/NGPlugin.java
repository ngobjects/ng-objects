package ng.plugins;

import java.util.Collections;
import java.util.List;

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
	 * @return The plugin's namespace. Defaults to the class's simple name.
	 *
	 * The given namespace will be used for locating resources and elements defined by this module.
	 */
	public default String namespace() {
		return getClass().getSimpleName();
	}

	/**
	 * @return Instructions on how to locate elements provided by this plugin
	 */
	public default Elements elements() {
		return Elements.create();
	}

	/**
	 * @return Definition of routes provided by this plugin
	 */
	public default Routes routes() {
		return Routes.create();
	}

	/**
	 * @return A list of plugin classes that this plugin requires (and potentially depends on being loaded and initialized before itself)
	 */
	public default List<Class<? extends NGPlugin>> requires() {
		return Collections.emptyList();
	}
}