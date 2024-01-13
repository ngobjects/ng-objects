package ng.appserver.privates;

/**
 * The ResourceTypes provided by default by the framework
 */

public enum StandardResourceType implements ResourceType {
	App, /* A resource meant to be consumed in java code */
	WebServer, /* A resource meant to be served to the world, images, CSS files and more */
	Public, /* Also resources that are served to the world, but that live within a Webserver root and thus get static URLs */
	ComponentTemplate; /* Template files for NGComponents */
}