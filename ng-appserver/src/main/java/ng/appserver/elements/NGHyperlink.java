package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGHyperlink extends NGDynamicGroup {

	private static final Logger logger = LoggerFactory.getLogger( NGHyperlink.class );

	private final NGAssociation _hrefAssociation;
	private final NGAssociation _actionAssociation;
	private final NGAssociation _pageNameAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGHyperlink( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_hrefAssociation = associations.get( "href" );
		_actionAssociation = associations.get( "action" );
		_pageNameAssociation = associations.get( "pageName" );

		// Now we collect the associations that we've already consumed and keep the rest around, to add to the image as attributes
		// Not exactly pretty, but let's work with this a little
		_additionalAssociations = new HashMap<>( associations );
		_additionalAssociations.remove( "href" );
		_additionalAssociations.remove( "action" );
		_additionalAssociations.remove( "pageName" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		String href = null;

		if( _hrefAssociation != null ) {
			href = (String)_hrefAssociation.valueInComponent( context.component() );
		}

		// FIXME: Work in progress
		if( _actionAssociation != null || _pageNameAssociation != null ) {
			final String senderID = context.contextID() + "." + context.elementID().toString();
			href = "/wo/" + senderID;
		}

		if( href == null ) {
			throw new IllegalStateException( "Failed to generate the href attribute for a hyperlink" );
		}

		StringBuilder startTag = new StringBuilder( "<a href=\"" + href + "\"" );

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

			/**
			 * FIXME: This isn't quite there yet // Hugi 2022-06-09
			 */
			if( _pageNameAssociation != null ) {
				final String pageName = (String)_pageNameAssociation.valueInComponent( context.component() );
				NGComponent actionResults = NGApplication.application().pageWithName( pageName, context );
				return actionResults;
			}
		}

		return null;
	}
}