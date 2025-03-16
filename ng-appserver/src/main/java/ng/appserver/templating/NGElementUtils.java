package ng.appserver.templating;

import ng.appserver.NGApplication;

/**
 * An abomination of a class that serves as a repository for temporary hacky stuff
 */

@Deprecated
public class NGElementUtils {

	/**
	 * Add a class to make searchable by it's simpleName, full class name or any of the given shortcuts (for tags)
	 */
	@Deprecated
	public static void addClass( final Class<?> elementClass, final String... tagNames ) {
		NGApplication.application().elementManager().registerElementClass( elementClass, tagNames );
	}

	@Deprecated
	public static void addPackage( final String packageName ) {
		NGApplication.application().elementManager().registerElementPackage( packageName );
	}
}