package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGString extends NGDynamicElement {

	private NGAssociation _value;

	public NGString( final String name, final Map<String, NGAssociation> associations, final NGElement template ) {
		super( name, associations, template );
		_value = associations.get( "value" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		Object value = _value.valueInComponent( context.component() );

		// FIXME: Missing valueWhenEmpty binding
		if( value == null ) {
			value = "";
		}

		response.appendContentString( value.toString() );
	}
}