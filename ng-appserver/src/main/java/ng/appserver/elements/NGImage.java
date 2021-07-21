package ng.appserver.elements;

import java.util.Map;
import java.util.Objects;

import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGImage extends NGDynamicElement {

	private final NGAssociation _filenameAssociation;

	public NGImage( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_filenameAssociation = associations.get( "filename" );
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		Objects.requireNonNull( response );
		Objects.requireNonNull( context );
		final NGComponent component = context.component();
		final String filename = (String)_filenameAssociation.valueInComponent( component );

		response.appendContentString( String.format( "<img src=\"%s\" />", filename ) );
	}
}
