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

public class NGTextField extends NGDynamicElement {

	/**
	 * 'name' attribute of the text field. If not specified, will be populated using the elementID
	 */
	private final NGAssociation _nameAssociation;

	/**
	 * The value for the field. This is a bidirectional binding that will also pass the value upstrem.
	 */
	private final NGAssociation _valueAssociation;

	public NGTextField( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_nameAssociation = associations.get( "name" );
		_valueAssociation = associations.get( "value" );
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		final List<String> valuesFromRequest = request.formValues().get( name( context ) );

		if( valuesFromRequest != null ) { // FIXME: Should formValues return an empty list or null if not present? // Hugi 2022-06-08
			String valueFromRequest = valuesFromRequest.get( 0 );
			_valueAssociation.setValue( valueFromRequest, context.component() );
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

		final String tagString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
		response.appendContentString( tagString );
	}

	private String name( final NGContext context ) {

		if( _nameAssociation != null ) {
			return (String)_nameAssociation.valueInComponent( context.component() );
		}

		return nameFromCurrentElementId( context );

	}

	/**
	 * @return A unique name for this text field, based on the NGContext's elementId
	 */
	private String nameFromCurrentElementId( final NGContext context ) {
		return context.elementID().toString();
	}
}