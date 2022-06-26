package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResourceRequestHandlerDynamic;
import ng.appserver.NGResponse;

/**
 * FIXME: Add a [mimeType] binding for use in conjunction with the [data] binding. Requires a better management of cached resources.
 */

public class NGImage extends NGDynamicElement {

	/**
	 * For keeping the filename of the image
	 */
	private final NGAssociation _dataAssociation;

	/**
	 * For keeping the filename of the image
	 */
	private final NGAssociation _filenameAssociation;

	/**
	 * The src of the image. We include this as a separate association so we can check if it's bound (if filename is missing)
	 */
	private final NGAssociation _srcAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGImage( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		super( name, associations, template );
		_filenameAssociation = associations.get( "filename" );
		_srcAssociation = associations.get( "src" );
		_dataAssociation = associations.get( "data" );

		if( _srcAssociation == null && _filenameAssociation == null && _dataAssociation == null ) {
			throw new IllegalArgumentException( "You must set [filename], [data] or [src] bindings" );
		}

		// Now we collect the associations that we've already consumed and keep the rest around, to add to the image as attributes
		// Not exactly pretty, but let's work with this a little
		_additionalAssociations = new HashMap<>( associations );
		_additionalAssociations.remove( "filename" );
		_additionalAssociations.remove( "data" );
		_additionalAssociations.remove( "src" );
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

		// In case of a data binding, we always just store the data in the resource cache, under a new key each time. Kind of lame.
		if( _dataAssociation != null ) {
			byte[] bytes = (byte[])_dataAssociation.valueInComponent( component );
			final String id = UUID.randomUUID().toString();
			NGResourceRequestHandlerDynamic.push( id, bytes );
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
