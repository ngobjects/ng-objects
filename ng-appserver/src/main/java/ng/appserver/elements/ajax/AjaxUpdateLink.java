package ng.appserver.elements.ajax;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGBindingConfigurationException;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.elements.NGDynamicGroup;

public class AjaxUpdateLink extends NGDynamicGroup {

	private static final Logger logger = LoggerFactory.getLogger( AjaxUpdateLink.class );

	private final NGAssociation _actionAssociation;
	private final NGAssociation _updateContainerIDAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public AjaxUpdateLink( String name, Map<String, NGAssociation> associations, NGElement element ) {
		super( name, associations, element );
		_additionalAssociations = new HashMap<>( associations );
		_actionAssociation = _additionalAssociations.remove( "action" );
		_updateContainerIDAssociation = _additionalAssociations.remove( "updateContainerID" );

		if( _actionAssociation == null ) {
			throw new NGBindingConfigurationException( "[action] is a required binding" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		final String onclick = "invokeUpdate(%s,'%s');return false;".formatted( updateContainerIDParameter( context ), context.componentActionURL() );

		final StringBuilder startTag = new StringBuilder( "<a href=\"#\" onclick=\"%s\"".formatted( onclick ) );

		if( !_additionalAssociations.isEmpty() ) {
			startTag.append( " " );

			_additionalAssociations.forEach( ( name, ass ) -> {
				startTag.append( " " );
				startTag.append( name );
				startTag.append( "=" );
				startTag.append( "\"" + ass.valueInComponent( context.component() ) + "\"" );
			} );
		}

		startTag.append( ">" );
		response.appendContentString( startTag.toString() );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</a>" );
	}

	/**
	 * @return The id of the updateContainer
	 */
	private String updateContainerIDParameter( NGContext context ) {
		if( _updateContainerIDAssociation != null ) {
			final String updateContainerID = (String)_updateContainerIDAssociation.valueInComponent( context.component() );
			return "'%s'".formatted( updateContainerID );
		}

		return "null";
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {

		if( context.elementID().equals( context.senderID() ) ) {
			if( _actionAssociation != null ) {
				NGActionResults result = (NGActionResults)_actionAssociation.valueInComponent( context.component() );
				logger.debug( "Action result is: " + result );
				return result;
			}
		}

		return null;
	}
}