package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGBindingConfigurationException;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;

/**
 * CHECKME: NGGenericElement and NGGenericContainer share a lot of code, we could probably simplify this // Hugi 2022-10-13
 */

public class NGGenericElement extends NGDynamicElement {

	private final NGAssociation elementNameAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGGenericElement( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
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

		// We might want to add a binding to specify if non-container elements are "closed" (i.e. a closing slash is added)
		b.append( " />" );

		response.appendContentString( b.toString() );
	}
}