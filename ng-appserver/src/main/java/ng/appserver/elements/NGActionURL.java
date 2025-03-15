package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGBindingConfigurationException;

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