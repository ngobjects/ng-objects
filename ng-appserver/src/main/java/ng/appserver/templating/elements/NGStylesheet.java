package ng.appserver.templating.elements;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.NGContext;
import ng.appserver.NGResourceRequestHandler;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGComponent;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;

/**
 * For embedding stylesheets in components
 */

public class NGStylesheet extends NGDynamicElement {

	private final NGAssociation _filenameAssociation;

	private final NGAssociation _namespaceAssociation;

	public NGStylesheet( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_filenameAssociation = associations.get( "filename" );
		_namespaceAssociation = NGHTMLUtilities.namespaceAssociation( associations, true );

		if( _filenameAssociation == null ) {
			throw new IllegalArgumentException( "'filename' is a required binding" );
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		Objects.requireNonNull( response );
		Objects.requireNonNull( context );
		final NGComponent component = context.component();

		final String filename = (String)_filenameAssociation.valueInComponent( component );
		final String namespace = NGHTMLUtilities.namespaceInContext( context, _namespaceAssociation );

		final Optional<String> relativeURL = NGResourceRequestHandler.urlForWebserverResourceNamed( namespace, filename );
		String urlString;

		if( relativeURL.isPresent() ) {
			urlString = relativeURL.get();
		}
		else {
			urlString = "ERROR_NOT_FOUND_" + filename;
		}

		response.appendContentString( String.format( "<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\" />", urlString ) );
	}
}