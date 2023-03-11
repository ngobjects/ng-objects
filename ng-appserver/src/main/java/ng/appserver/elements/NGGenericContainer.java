package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGBindingConfigurationException;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGGenericContainer extends NGDynamicGroup {

	private final NGAssociation elementNameAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGGenericContainer( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_additionalAssociations = new HashMap<>( associations );

		elementNameAssociation = _additionalAssociations.remove( "elementName" );

		if( elementNameAssociation == null ) {
			throw new NGBindingConfigurationException( "elementName is a required binding" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final String elementName = (String)elementNameAssociation.valueInComponent( context.component() );

		final StringBuilder b = new StringBuilder();

		b.append( "<" + elementName );

		_additionalAssociations.forEach( ( name, ass ) -> {
			final Object value = ass.valueInComponent( context.component() );

			if( value != null ) {
				b.append( " " );
				b.append( name );
				b.append( "=" );
				b.append( "\"" + value + "\"" );
			}
		} );

		b.append( ">" );

		response.appendContentString( b.toString() );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</" + elementName + ">" );
	}
}