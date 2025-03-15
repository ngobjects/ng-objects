package ng.appserver.elements;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGResourceRequestHandler;
import ng.appserver.NGResourceRequestHandlerDynamic;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.resources.NGDynamicResource;
import ng.appserver.templating.NGBindingConfigurationException;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;

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
	 * The filename of an image resource
	 */
	private final NGAssociation _filenameAssociation;

	/**
	 * The namespace of an image supplied via [filename]
	 */
	private final NGAssociation _namespaceAssociation;

	/**
	 * The src of the image. We include this as a separate association so we can check if it's bound (if filename is missing)
	 */
	private final NGAssociation _srcAssociation;

	/**
	 * The mimeType of an image supplied via [data] or [dataInputStream]
	 */
	private final NGAssociation _mimeTypeAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGImage( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		super( null, null, null );
		_additionalAssociations = new HashMap<>( associations );
		_filenameAssociation = _additionalAssociations.remove( "filename" );
		_namespaceAssociation = NGHTMLUtilities.namespaceAssociation( associations, true );
		_srcAssociation = _additionalAssociations.remove( "src" );
		_dataAssociation = _additionalAssociations.remove( "data" );
		_dataInputStreamAssociation = _additionalAssociations.remove( "dataInputStream" );
		_dataInputStreamLengthAssociation = _additionalAssociations.remove( "dataInputStreamLength" );
		_mimeTypeAssociation = _additionalAssociations.remove( "mimeType" );

		if( _srcAssociation == null && _filenameAssociation == null && _dataAssociation == null && _dataInputStreamAssociation == null ) {
			throw new NGBindingConfigurationException( "You must set [filename], [data], [dataInputStream] or [src] bindings" );
		}

		if( (_dataAssociation != null || _dataInputStreamAssociation != null) && _mimeTypeAssociation == null ) {
			throw new NGBindingConfigurationException( "You must set [mimeType] if using [data] or [dataInputStream]" );
		}

		if( _dataInputStreamAssociation != null && _dataInputStreamLengthAssociation == null ) {
			throw new NGBindingConfigurationException( "If [dataInputStream] is bound, you must also bind [dataInputStreamLength]" );
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		Objects.requireNonNull( response );
		Objects.requireNonNull( context );

		final NGComponent component = context.component();
		String src = null;

		if( _filenameAssociation != null ) {
			final String filename = (String)_filenameAssociation.valueInComponent( component );
			final String namespace = NGHTMLUtilities.namespaceInContext( context, _namespaceAssociation );
			final Optional<String> relativeURL = NGResourceRequestHandler.urlForWebserverResourceNamed( namespace, filename );

			if( relativeURL.isPresent() ) {
				src = relativeURL.get();
			}
			else {
				src = "ERROR_NOT_FOUND_" + filename;
			}
		}

		// In case of a [data] or [dataInputStream] binding, we create a resource in the resource cache under a new key each time.
		if( _dataAssociation != null || _dataInputStreamAssociation != null ) {
			final String mimeType = (String)_mimeTypeAssociation.valueInComponent( component );

			// we obtain a key that we're going to store the cached resource under in the ResourceManager's cache
			final String resourceCacheKey = UUID.randomUUID().toString();

			final InputStream inputStream;
			final Long length;

			if( _dataAssociation != null ) {
				byte[] bytes = (byte[])_dataAssociation.valueInComponent( component );

				if( bytes == null ) {
					bytes = new byte[] {};
				}

				inputStream = new ByteArrayInputStream( bytes );
				length = (long)bytes.length;
			}
			else {
				inputStream = (InputStream)_dataInputStreamAssociation.valueInComponent( component );
				length = (long)_dataInputStreamLengthAssociation.valueInComponent( component );
			}

			final NGDynamicResource resource = new NGDynamicResource( resourceCacheKey, inputStream, mimeType, length );
			NGApplication.application().resourceManagerDynamic().push( resourceCacheKey, resource );
			src = NGResourceRequestHandlerDynamic.urlForDynamicResourceNamed( resourceCacheKey ).get();
		}

		if( _srcAssociation != null ) {
			src = (String)_srcAssociation.valueInComponent( context.component() );
		}

		final Map<String, String> properties = new HashMap<>();
		properties.put( "src", src );
		NGHTMLUtilities.addAssociationValuesToAttributes( properties, _additionalAssociations, component );

		final String tagString = NGHTMLUtilities.createElementStringWithAttributes( "img", properties, true );

		response.appendContentString( tagString );
	}
}