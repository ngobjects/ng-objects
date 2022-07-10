package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGBindingConfigurationException;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGGenericContainer extends NGDynamicGroup {

	private NGAssociation elementNameAssociation;

	public NGGenericContainer( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		elementNameAssociation = associations.get( "elementName" );

		if( elementNameAssociation == null ) {
			throw new NGBindingConfigurationException( "elementName is a required binding" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final String elementName = (String)elementNameAssociation.valueInComponent( context.component() );

		response.appendContentString( "<" + elementName + ">" );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</" + elementName + ">" );
	}
}