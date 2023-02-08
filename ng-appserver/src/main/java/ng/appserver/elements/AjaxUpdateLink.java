package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

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
		_actionAssociation = associations.get( "action" );
		_updateContainerIDAssociation = associations.get( "updateContainerID" );

		// Now we collect the associations that we've already consumed and keep the rest around, to add to the image as attributes
		// Not exactly pretty, but let's work with this a little
		_additionalAssociations = new HashMap<>( associations );
		_additionalAssociations.remove( "action" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		String href = null;

		if( _actionAssociation != null ) {
			href = context.componentActionURL();
		}

		if( href == null ) {
			throw new IllegalStateException( "Failed to generate the href attribute for a hyperlink" );
		}

		String updateContainerID = (String)_updateContainerIDAssociation.valueInComponent( context.component() );

		String onclick = "invokeUpdate('%s','%s');return false;".formatted( updateContainerID, context.componentActionURL() );

		StringBuilder startTag = new StringBuilder( "<a href=\"#\" onclick=\"%s\"".formatted( onclick ) );

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

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {

		if( context.elementID().toString().equals( context.senderID() ) ) {
			logger.debug( "invokeAction() : current contextID is: " + context.contextID() );
			logger.debug( "invokeAction() : current elementID is: " + context.elementID() );
			logger.debug( "invokeAction() : current senderID is: " + context.senderID() );
			logger.debug( "invokeAction() : current component is: " + context.component() );

			if( _actionAssociation != null ) {
				NGActionResults result = (NGActionResults)_actionAssociation.valueInComponent( context.component() );
				logger.debug( "Action result is: " + result );
				return result;
			}
		}

		return null;
	}
}