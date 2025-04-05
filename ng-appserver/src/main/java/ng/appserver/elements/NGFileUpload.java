package ng.appserver.elements;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGRequest.UploadedFile;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;

public class NGFileUpload extends NGDynamicElement {

	private final NGAssociation _filenameAssociation;
	private final NGAssociation _dataAssociation;
	private final NGAssociation _nameAssociation;

	public NGFileUpload( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		super( null, null, null );
		_filenameAssociation = associations.get( "filename" );
		_dataAssociation = associations.get( "data" );
		_nameAssociation = associations.get( "name" );
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		response.appendContentString( """
					<input type="file" name="%s" />
				""".formatted( name( context ) ) );
	}

	/**
	 * @return The name of the field (to use in the HTML code)
	 */
	private String name( final NGContext context ) {

		if( _nameAssociation != null ) {
			return (String)_nameAssociation.valueInComponent( context.component() );
		}

		return context.elementID().toString();
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		final String name = name( context );
		final List<String> valuesFromRequest = request.formValuesForKey( name );

		if( !valuesFromRequest.isEmpty() ) {

			// If multiple form values are present for the same field name, the potential for an error condition is probably high enough to just go ahead and fail.
			if( valuesFromRequest.size() > 1 ) {
				throw new IllegalStateException( "The request contains %s form values named '%s'. I can only handle one at a time. The values you sent me are (%s).".formatted( valuesFromRequest.size(), name, valuesFromRequest ) );
			}

			// If we have a value, that's the filename
			final String filename = valuesFromRequest.get( 0 );

			_filenameAssociation.setValue( filename, context.component() );

			if( _dataAssociation != null ) {
				final UploadedFile file = request._uploadedFiles().get( filename );

				try {
					byte[] data = file.stream().readAllBytes();
					_dataAssociation.setValue( data, context.component() );
				}
				catch( IOException e ) {
					throw new RuntimeException( e );
				}
			}
		}
	}
}