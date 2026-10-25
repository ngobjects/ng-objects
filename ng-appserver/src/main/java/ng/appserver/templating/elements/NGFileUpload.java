package ng.appserver.templating.elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.http.NGMultipartRequest;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGResponse;
import ng.appserver.http.NGUploadedFile;
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
		super( null, null, null );
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

		if( !(request instanceof NGMultipartRequest multipartRequest) ) {

			// A form that isn't multipart submits a file field's filename as a plain value, and the file itself not at all
			if( !request.formValuesForKey( name ).isEmpty() ) {
				throw new IllegalStateException( "The form submitted the file upload field '%s' without its file. A form containing a file upload needs the attribute enctype=\"multipart/form-data\"".formatted( name ) );
			}

			return;
		}

		final List<NGUploadedFile> files = multipartRequest.uploadedFilesForKey( name );

		if( !files.isEmpty() ) {

			// If multiple files are present for the same field name, the potential for an error condition is probably high enough to just go ahead and fail.
			if( files.size() > 1 ) {
				throw new IllegalStateException( "The request contains %s files uploaded with the field '%s'. I can only handle one at a time.".formatted( files.size(), name ) );
			}

			final NGUploadedFile file = files.get( 0 );

			if( _filenameAssociation != null ) {
				_filenameAssociation.setValue( file.filename(), context.component() );
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
