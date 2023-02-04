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
 * For embedding scripts in components
 */

public class NGScript extends NGDynamicElement {

	private NGAssociation _srcAssociation;

	public NGScript( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_srcAssociation = associations.get( "src" );

		if( _srcAssociation == null ) {
			throw new IllegalArgumentException( "'src' is a required binding" );
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		Objects.requireNonNull( response );
		Objects.requireNonNull( context );
		final NGComponent component = context.component();
		final String filename = (String)_srcAssociation.valueInComponent( component );
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