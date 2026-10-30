package ng.appserver.templating.elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGResponse;
import ng.appserver.http.NGStandardRequest.UploadedFile;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;

public class NGFileUpload extends NGDynamicElement {

	private final NGAssociation _filenameAssociation;
	private final NGAssociation _dataAssociation;
	private final NGAssociation _nameAssociation;

	/**
	 * For storing associations that aren't part of the element's basic associations, rendered as attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGFileUpload( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		super( name, associations, contentTemplate );
		_additionalAssociations = new LinkedHashMap<>( associations );
		_filenameAssociation = _additionalAssociations.remove( "filename" );
		_dataAssociation = _additionalAssociations.remove( "data" );
		_nameAssociation = _additionalAssociations.remove( "name" );
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		final Map<String, String> attributes = new LinkedHashMap<>();
		attributes.put( "type", "file" );
		attributes.put( "name", String.valueOf( name( context ) ) );
		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		response.appendContentString( NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true ) );
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
			final String fileID = valuesFromRequest.get( 0 );

			final UploadedFile file = request._uploadedFiles().get( fileID );

			if( file == null ) {
				throw new IllegalStateException( "No file with ID %s found in request".formatted( fileID ) );
			}

			if( _filenameAssociation != null ) {
				_filenameAssociation.setValue( file.name(), context.component() );
			}

			if( _dataAssociation != null ) {
				try( final InputStream stream = file.stream()) {
					_dataAssociation.setValue( stream.readAllBytes(), context.component() );
				}
				catch( IOException e ) {
					throw new RuntimeException( e );
				}
			}
		}
	}
}