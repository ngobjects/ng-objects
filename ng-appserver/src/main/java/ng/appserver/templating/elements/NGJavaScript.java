package ng.appserver.templating.elements;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.NGContext;
import ng.appserver.NGResourceRequestHandler;
import ng.appserver.http.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGComponent;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;

/**
 * For embedding JavaScript files in components
 */

public class NGJavaScript extends NGDynamicElement {

	private final NGAssociation _filenameAssociation;

	private final NGAssociation _namespaceAssociation;

	/**
	 * For storing associations that aren't part of the element's basic associations, rendered as attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGJavaScript( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_additionalAssociations = new LinkedHashMap<>( associations );
		_filenameAssociation = _additionalAssociations.remove( "filename" );
		_namespaceAssociation = NGHTMLUtilities.namespaceAssociation( _additionalAssociations, true );

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

		final Map<String, String> attributes = new LinkedHashMap<>();
		attributes.put( "src", urlString );
		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, component );

		response.appendContentString( NGHTMLUtilities.createElementStringWithAttributes( "script", attributes, false ) );
		response.appendContentString( "</script>" );
	}
}