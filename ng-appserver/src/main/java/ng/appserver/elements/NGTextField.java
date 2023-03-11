package ng.appserver.elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.privates._NGUtilities;

public class NGTextField extends NGDynamicElement {

	/**
	 * 'name' attribute of the text field. If not specified, will be populated using the elementID
	 */
	private final NGAssociation _nameAssociation;

	/**
	 * The value for the field. This is a bidirectional binding that will also pass the value upstrem.
	 */
	private final NGAssociation _valueAssociation;

	/**
	 * Indicates that the text field is disabled.
	 *
	 * Both actually disables the text fields, and prevents values from it from being read.
	 */
	private final NGAssociation _disabledAssociation;

	public NGTextField( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_nameAssociation = associations.get( "name" );
		_valueAssociation = associations.get( "value" );
		_disabledAssociation = associations.get( "disabled" );
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		if( !disabled( context ) ) {
			final List<String> valuesFromRequest = request.formValues().get( name( context ) );

			// FIXME: Should formValues return an empty list or null if not present? // Hugi 2022-06-08
			// FIXME: We should probably warn or even fail/throw if multiple values are present? // Hugi 2023-03-11
			if( valuesFromRequest != null ) {
				String valueFromRequest = valuesFromRequest.get( 0 );
				_valueAssociation.setValue( valueFromRequest, context.component() );
			}
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		final String value = (String)_valueAssociation.valueInComponent( context.component() );

		final Map<String, String> attributes = new HashMap<>();

		attributes.put( "type", "text" );
		attributes.put( "name", name( context ) );

		if( value != null ) {
			attributes.put( "value", value );
		}

		if( disabled( context ) ) {
			// FIXME: 'disabled' is a "boolean attribute" and doesn't really need a value. We need a nice way to generate those // Hugi 2023-03-11
			attributes.put( "disabled", "" );
		}

		final String tagString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
		response.appendContentString( tagString );
	}

	/**
	 * @return True if the field is disabled
	 */
	private boolean disabled( final NGContext context ) {
		if( _disabledAssociation != null ) {
			return _NGUtilities.isTruthy( _disabledAssociation.valueInComponent( context.component() ) );
		}

		return false;
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
}