package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGHyperlink extends NGDynamicGroup {

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

		// An href-binding gets passed directly to the link.
		if( _hrefAssociation != null ) {
			href = (String)_hrefAssociation.valueInComponent( context.component() );
		}

		if( _actionAssociation != null || _pageNameAssociation != null ) {
			href = context.componentActionURL();
		}

		if( href == null ) {
			throw new IllegalStateException( "Failed to generate the href attribute for a hyperlink" );
		}

		final StringBuilder tagString = new StringBuilder( "<a href=\"" + href + "\"" );

		if( !_additionalAssociations.isEmpty() ) {
			tagString.append( " " );

			_additionalAssociations.forEach( ( name, ass ) -> {
				tagString.append( " " );
				tagString.append( name );
				tagString.append( "=" );
				tagString.append( "\"" + ass.valueInComponent( context.component() ) + "\"" );
			} );
		}

		tagString.append( ">" );
		response.appendContentString( tagString.toString() );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</a>" );
	}

	@Override
	public NGActionResults invokeAction( final NGRequest request, final NGContext context ) {

		if( context.currentElementIsSender() ) {

			if( _actionAssociation != null ) {
				return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
			}

			if( _pageNameAssociation != null ) {
				final String pageName = (String)_pageNameAssociation.valueInComponent( context.component() );
				final NGComponent actionResults = NGApplication.application().pageWithName( pageName, context );
				return actionResults;
			}
		}

		return null;
	}
}