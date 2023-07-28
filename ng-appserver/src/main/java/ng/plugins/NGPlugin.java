package ng.plugins;

/**
 * Defines an extension point/module to be used with the framework
 */

public abstract class NGPlugin {

	/**
	 * Executed at application startup to initialize the plugin.
	 *
	 * CHECKME: We should be specifiying at exactly _which_ point in application startup this gets invoked.
	 */
	public abstract void load();
}