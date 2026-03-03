package ng.appserver.templating.elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.associations.NGAssociationUtils;

public class NGCheckbox extends NGDynamicElement {

	/**
	 * 'name' attribute of the checkbox. If not specified, will be populated using the elementID
	 */
	private final NGAssociation _nameAssociation;

	/**
	 * The checked state of the checkbox. This is a bidirectional binding (boolean).
	 */
	private final NGAssociation _checkedAssociation;

	/**
	 * Indicates that the checkbox is disabled.
	 */
	private final NGAssociation _disabledAssociation;

	/**
	 * Pass-through attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGCheckbox( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_additionalAssociations = new HashMap<>( associations );
		_nameAssociation = _additionalAssociations.remove( "name" );
		_checkedAssociation = _additionalAssociations.remove( "checked" );
		_disabledAssociation = _additionalAssociations.remove( "disabled" );
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		if( !disabled( context ) ) {
			final String name = name( context );
			final List<String> valuesFromRequest = request.formValuesForKey( name );

			// For checkboxes, presence of the form value means checked, absence means unchecked.
			// This is different from text fields where absence means "not submitted".
			final boolean checked = !valuesFromRequest.isEmpty();
			_checkedAssociation.setValue( checked, context.component() );
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		final Map<String, String> attributes = new HashMap<>();

		attributes.put( "type", "checkbox" );
		attributes.put( "name", name( context ) );

		final Object checkedValue = _checkedAssociation.valueInComponent( context.component() );

		if( NGAssociationUtils.isTruthy( checkedValue ) ) {
			attributes.put( "checked", "checked" );
		}

		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		if( disabled( context ) ) {
			attributes.put( "disabled", "" );
		}

		final String tagString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
		response.appendContentString( tagString );
	}

	/**
	 * @return True if the checkbox is disabled
	 */
	private boolean disabled( final NGContext context ) {
		if( _disabledAssociation != null ) {
			return NGAssociationUtils.isTruthy( _disabledAssociation.valueInComponent( context.component() ) );
		}

		return false;
	}

	/**
	 * @return The name of the checkbox (to use in the HTML code)
	 */
	private String name( final NGContext context ) {

		if( _nameAssociation != null ) {
			return (String)_nameAssociation.valueInComponent( context.component() );
		}

		return context.elementID().toString();
	}
}
