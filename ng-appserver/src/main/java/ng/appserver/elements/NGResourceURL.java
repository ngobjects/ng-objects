package ng.appserver.elements;

import java.util.Map;
import java.util.Optional;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResourceRequestHandler;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;

public class NGResourceURL extends NGDynamicElement {

	private final NGAssociation _namespaceAssociation;

	private final NGAssociation _filenameAssociation;

	public NGResourceURL( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		super( null, null, null );
		_namespaceAssociation = NGHTMLUtilities.namespaceAssociation( associations, true );
		_filenameAssociation = associations.get( "filename" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final String filename = (String)_filenameAssociation.valueInComponent( context.component() );
		final String namespace = NGHTMLUtilities.namespaceInContext( context, _namespaceAssociation );
		final Optional<String> url = NGResourceRequestHandler.urlForWebserverResourceNamed( namespace, filename );

		// FIXME: We might want some more graceful error handling here in case of a non-existent resource // Hugi 2024-10-22
		response.appendContentString( url.get() );
	}
}