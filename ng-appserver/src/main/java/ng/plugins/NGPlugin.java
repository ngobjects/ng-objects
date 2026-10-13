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
	 * @return Ignored: a namespace's elements are declared in its components folder, and found on the classpath by name
	 *
	 * FIXME: Delete before 0.1.6 is released, with Elements. Kept so plugins that still override it compile while they're cleaned out // Hugi 2026-10-14
	 */
	@Deprecated
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
	 * @return The plugins this plugin requires: they're initialized and loaded before it, and
	 * application startup fails if one isn't present
	 */
	public default List<Class<? extends NGPlugin>> requires() {
		return Collections.emptyList();
	}
}