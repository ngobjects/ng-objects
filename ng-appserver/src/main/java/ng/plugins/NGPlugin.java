package ng.plugins;

import ng.appserver.NGApplication;

/**
 * Defines an extension point/module to be used with the framework
 */

public abstract class NGPlugin {

	/**
	 * Executed at application startup to initialize the plugin.
	 */
	public abstract void load( NGApplication application );
}