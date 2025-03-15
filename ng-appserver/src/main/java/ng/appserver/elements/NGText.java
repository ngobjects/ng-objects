package ng.appserver.elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGBindingConfigurationException;
import ng.appserver.templating.assications.NGAssociation;

/**
 * A text area
 */

public class NGText extends NGDynamicElement {

	/**
	 * 'name' attribute of the text field. If not specified, will be populated using the elementID
	 */
	private final NGAssociation _nameAssociation;

	/**
	 * The value for the field. This is a bidirectional binding that will also pass the value upstrem.
	 */
	private final NGAssociation _valueAssociation;

	/**
	 * Pass-through attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGText( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_additionalAssociations = new HashMap<>( associations );
		_nameAssociation = _additionalAssociations.remove( "name" );
		_valueAssociation = _additionalAssociations.remove( "value" );

		if( _valueAssociation == null ) {
			throw new NGBindingConfigurationException( "[value] binding is required" );
		}
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		final String name = NGDynamicElementUtils.name( _nameAssociation, context );
		final List<String> valuesFromRequest = request.formValuesForKey( name );

		if( !valuesFromRequest.isEmpty() ) {

			// If multiple form values are present for the same field name, the potential for an error condition is probably high enough to just go ahead and fail.
			if( valuesFromRequest.size() > 1 ) {
				throw new IllegalStateException( "The request contains %s form values named '%s'. I can only handle one at a time. The values you sent me are (%s).".formatted( valuesFromRequest.size(), name, valuesFromRequest ) );
			}

			Object value = null; // We're sticking with null as the default for an empty string to align with WO behaviour. This might have to be revisited in the future.

			final String stringValueFromRequest = valuesFromRequest.get( 0 );

			if( !stringValueFromRequest.isEmpty() ) {
				value = stringValueFromRequest;
			}

			_valueAssociation.setValue( value, context.component() );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final Object value = _valueAssociation.valueInComponent( context.component() );

		final Map<String, String> attributes = new HashMap<>();
		attributes.put( "name", NGDynamicElementUtils.name( _nameAssociation, context ) );

		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		response.appendContentString( NGHTMLUtilities.createElementStringWithAttributes( "textarea", attributes, false ) );

		if( value != null ) {
			final String escapedValue = NGHTMLUtilities.escapeHTML( value.toString() );
			response.appendContentString( escapedValue );
		}

		response.appendContentString( "</textarea>" );
	}
}