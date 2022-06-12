package ng.appserver.elements;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

/**
 * For embedding stylesheets in components
 */

public class NGJavaScript extends NGDynamicElement {

	private NGAssociation _filenameAssociation;

	public NGJavaScript( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_filenameAssociation = associations.get( "filename" );
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		Objects.requireNonNull( response );
		Objects.requireNonNull( context );
		final NGComponent component = context.component();
		final String filename = (String)_filenameAssociation.valueInComponent( component );
		final Optional<String> relativeURL = NGApplication.application().resourceManager().urlForWebserverResourceNamed( filename );
		String urlString;

		if( relativeURL.isPresent() ) {
			urlString = relativeURL.get();
		}
		else {
			urlString = "ERROR_NOT_FOUND_" + filename;
		}

		response.appendContentString( String.format( "<script src=\"%s\"></script>", urlString ) );
	}
}