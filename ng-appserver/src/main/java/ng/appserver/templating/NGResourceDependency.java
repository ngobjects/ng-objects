package ng.appserver.templating;

import java.util.Objects;

import ng.appserver.NGResourceRequestHandler;

/**
 * A webserver resource an element needs on the page it's rendered in, declared by overriding NGElement.resourceDependencies().
 *
 * When a full page has rendered, the framework adds a tag for each declared dependency to the page's head, once per page,
 * skipping those the page already includes through <wo:script> or <wo:stylesheet>.
 *
 * FIXME: Partial page responses don't carry dependencies yet, so a dependency first rendered by an update container update never reaches the page // Hugi 2026-11-03
 *
 * @param kind What kind of tag the resource needs
 * @param namespace The namespace of the resource
 * @param filename The resource's path within the namespace's webserver resources
 */
public record NGResourceDependency( Kind kind, String namespace, String filename ) {

	public enum Kind {
		Script,
		Stylesheet
	}

	public NGResourceDependency {
		Objects.requireNonNull( kind );
		Objects.requireNonNull( namespace );
		Objects.requireNonNull( filename );
	}

	/**
	 * @return A dependency on the given JavaScript file
	 */
	public static NGResourceDependency script( final String namespace, final String filename ) {
		return new NGResourceDependency( Kind.Script, namespace, filename );
	}

	/**
	 * @return A dependency on the given stylesheet
	 */
	public static NGResourceDependency stylesheet( final String namespace, final String filename ) {
		return new NGResourceDependency( Kind.Stylesheet, namespace, filename );
	}

	/**
	 * @return The HTML tag that includes the resource in a page
	 */
	public String tag() {
		final String url = NGResourceRequestHandler
				.urlForWebserverResourceNamed( namespace, filename )
				.orElse( "ERROR_NOT_FOUND_" + filename );

		return switch( kind ) {
			case Script -> "<script src=\"%s\"></script>".formatted( url );
			case Stylesheet -> "<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\" />".formatted( url );
		};
	}
}
