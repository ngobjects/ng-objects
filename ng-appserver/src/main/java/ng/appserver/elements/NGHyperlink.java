package ng.appserver.elements;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGHyperlink extends NGDynamicGroup {

	private static final Logger logger = LoggerFactory.getLogger( NGHyperlink.class );

	private final NGAssociation _hrefAssociation;
	private final NGAssociation _actionAssociation;

	public NGHyperlink( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_hrefAssociation = associations.get( "href" );
		_actionAssociation = associations.get( "action" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		String href = null;

		if( _hrefAssociation != null ) {
			href = (String)_hrefAssociation.valueInComponent( context.component() );
		}

		String senderID = context.contextID() + "." + context.elementID().toString();

		// FIXME: Work in progress
		if( _actionAssociation != null ) {
			href = "/wo/" + senderID;
		}

		if( href == null ) {
			throw new IllegalStateException( "Failed to generate the href attribute for a hyperlink" );
		}

		response.appendContentString( "<a href=\"" + href + "\">" );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</a>" );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {

		logger.debug( "invokeAction() : current contextID is: " + context.contextID() );
		logger.debug( "invokeAction() : current elementID is: " + context.elementID() );
		logger.debug( "invokeAction() : current senderID is: " + context.senderID() );
		logger.debug( "invokeAction() : current component is: " + context.component() );

		if( context.elementID().toString().equals( context.senderID() ) ) {
			return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
		}

		return null;
	}
}