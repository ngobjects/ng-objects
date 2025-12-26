package ng.appserver.templating.elements;

import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGBindingConfigurationException;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;

public class NGActionURL extends NGDynamicElement {

	private final NGAssociation _actionAssociation;

	public NGActionURL( String name, Map<String, NGAssociation> associations, NGElement contentTemplate ) {
		super( null, null, null );
		_actionAssociation = associations.get( "action" );

		if( _actionAssociation == null ) {
			throw new NGBindingConfigurationException( "'action' is a required binding" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		response.appendContentString( context.componentActionURL() );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		if( context.currentElementIsSender() ) {
			return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
		}

		return null;
	}
}