package ng.appserver.elements;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGBindingConfigurationException;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResourceRequestHandlerDynamic;
import ng.appserver.NGResourceRequestHandlerDynamic.NGDynamicResource;
import ng.appserver.NGResponse;

/**
 * An image element
 */

public class NGImage extends NGDynamicElement {

	/**
	 * For keeping the filename of the image
	 */
	private final NGAssociation _dataAssociation;

	/**
	 * For keeping the filename of the image
	 */
	private final NGAssociation _dataInputStreamAssociation;

	/**
	 * For keeping the filename of the image
	 */
	private final NGAssociation _dataInputStreamLengthAssociation;

	/**
	 * For keeping the filename of the image
	 */
	private final NGAssociation _filenameAssociation;

	/**
	 * The src of the image. We include this as a separate association so we can check if it's bound (if filename is missing)
	 */
	private final NGAssociation _srcAssociation;

	/**
	 * THe mimeType of an image supplied via [data] or [dataInputStream]
	 */
	private final NGAssociation _mimeTypeAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGImage( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		super( null, null, null );
		_filenameAssociation = associations.get( "filename" );
		_srcAssociation = associations.get( "src" );
		_dataAssociation = associations.get( "data" );
		_dataInputStreamAssociation = associations.get( "dataInputStream" );
		_dataInputStreamLengthAssociation = associations.get( "dataInputStreamLength" );
		_mimeTypeAssociation = associations.get( "mimeType" );

		if( _srcAssociation == null && _filenameAssociation == null && _dataAssociation == null && _dataInputStreamAssociation == null ) {
			throw new NGBindingConfigurationException( "You must set [filename], [data], [dataInputStream] or [src] bindings" );
		}

		if( (_dataAssociation != null || _dataInputStreamAssociation != null) && _mimeTypeAssociation == null ) {
			throw new NGBindingConfigurationException( "You must set [mimeType] if using [data] or [dataInputStream]" );
		}

		if( _dataInputStreamAssociation != null && _dataInputStreamLengthAssociation == null ) {
			throw new NGBindingConfigurationException( "If [dataInputStream] is bound, you must also bind [dataInputStreamLength]" );
		}

		// Now we collect the associations that we've already consumed and keep the rest around, to add to the image as attributes
		// Not exactly pretty, but let's work with this a little
		_additionalAssociations = new HashMap<>( associations );
		_additionalAssociations.remove( "filename" );
		_additionalAssociations.remove( "src" );
		_additionalAssociations.remove( "data" );
		_additionalAssociations.remove( "dataInputStream" );
		_additionalAssociations.remove( "dataInputStreamLength" );
		_additionalAssociations.remove( "mimeType" );
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		Objects.requireNonNull( response );
		Objects.requireNonNull( context );

		final NGComponent component = context.component();
		String src = null;

		if( _filenameAssociation != null ) {
			final String filename = (String)_filenameAssociation.valueInComponent( component );
			final Optional<String> relativeURL = NGApplication.application().resourceManager().urlForWebserverResourceNamed( filename );

			if( relativeURL.isPresent() ) {
				src = relativeURL.get();
			}
			else {
				src = "ERROR_NOT_FOUND_" + filename;
			}
		}

		final String mimeType = (String)_mimeTypeAssociation.valueInComponent( component );

		// In case of a data binding, we always just store the data in the resource cache, under a new key each time. Kind of lame.
		if( _dataAssociation != null ) {
			byte[] bytes = (byte[])_dataAssociation.valueInComponent( component );
			final String id = UUID.randomUUID().toString();
			final NGDynamicResource resource = new NGDynamicResource( new ByteArrayInputStream( bytes ), mimeType, (long)bytes.length );
			NGResourceRequestHandlerDynamic.push( id, resource );
			src = NGApplication.application().resourceManager().urlForDynamicResourceNamed( id ).get();
		}

		// FIXME: Lots of code duplication from the 'data' binding handling, refactor and consolidate once we have a semi-nice structure for this // Hugi 2023-02-10
		if( _dataInputStreamAssociation != null ) {
			long dataInputStreamLength = (long)_dataInputStreamLengthAssociation.valueInComponent( component );
			final InputStream is = (InputStream)_dataInputStreamAssociation.valueInComponent( component );
			final String id = UUID.randomUUID().toString();
			final NGDynamicResource resource = new NGDynamicResource( is, mimeType, dataInputStreamLength );
			NGResourceRequestHandlerDynamic.push( id, resource );
			src = NGApplication.application().resourceManager().urlForDynamicResourceNamed( id ).get();
		}

		if( _srcAssociation != null ) {
			src = (String)_srcAssociation.valueInComponent( context.component() );
		}

		final StringBuilder b = new StringBuilder();
		b.append( String.format( "<img src=\"%s\"", src ) );

		_additionalAssociations.forEach( ( name, ass ) -> {
			b.append( " " );
			b.append( name );
			b.append( "=" );
			b.append( "\"" + ass.valueInComponent( component ) + "\"" );
		} );

		b.append( " />" );

		response.appendContentString( b.toString() );
	}
}