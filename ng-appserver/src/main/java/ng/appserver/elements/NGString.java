package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGString extends NGDynamicElement {

	/**
	 * The string's value
	 */
	private NGAssociation _valueAss;

	/**
	 * A value to display if the string is null or empty (length==0)
	 */
	private NGAssociation _valueWhenEmptyAss;

	public NGString( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		super( null, null, null );
		_valueAss = associations.get( "value" );
		_valueWhenEmptyAss = associations.get( "valueWhenEmpty" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		Object value = _valueAss.valueInComponent( context.component() );

		if( value == null ) {
			value = "";
		}

		if( _valueWhenEmptyAss != null ) {
			if( value == null || (value instanceof String s && s.isEmpty()) ) {
				value = _valueWhenEmptyAss.valueInComponent( context.component() );
			}
		}

		response.appendContentString( value.toString() );
	}
}