package ng.appserver.templating.elements;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.associations.NGAssociationUtils;

/**
 * A password input field. Identical to NGTextField but renders type="password" and never sends the value back to the browser.
 */

public class NGPasswordField extends NGDynamicElement {

	private final NGAssociation _nameAssociation;
	private final NGAssociation _valueAssociation;
	private final NGAssociation _disabledAssociation;
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGPasswordField( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_additionalAssociations = new HashMap<>( associations );
		_nameAssociation = _additionalAssociations.remove( "name" );
		_valueAssociation = _additionalAssociations.remove( "value" );
		_disabledAssociation = _additionalAssociations.remove( "disabled" );
	}

	@Override
	public void takeValuesFromRequest( ng.appserver.NGRequest request, NGContext context ) {
		if( !disabled( context ) ) {
			final String name = name( context );
			final java.util.List<String> valuesFromRequest = request.formValuesForKey( name );

			if( !valuesFromRequest.isEmpty() ) {
				if( valuesFromRequest.size() > 1 ) {
					throw new IllegalStateException( "The request contains %s form values named '%s'. I can only handle one at a time.".formatted( valuesFromRequest.size(), name ) );
				}

				final String stringValue = valuesFromRequest.get( 0 );
				_valueAssociation.setValue( stringValue.isEmpty() ? null : stringValue, context.component() );
			}
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		final Map<String, String> attributes = new HashMap<>();

		attributes.put( "type", "password" );
		attributes.put( "name", name( context ) );

		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		if( disabled( context ) ) {
			attributes.put( "disabled", "" );
		}

		final String tagString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
		response.appendContentString( tagString );
	}

	private boolean disabled( final NGContext context ) {
		if( _disabledAssociation != null ) {
			return NGAssociationUtils.isTruthy( _disabledAssociation.valueInComponent( context.component() ) );
		}

		return false;
	}

	private String name( final NGContext context ) {
		if( _nameAssociation != null ) {
			return (String)_nameAssociation.valueInComponent( context.component() );
		}

		return context.elementID().toString();
	}
}
