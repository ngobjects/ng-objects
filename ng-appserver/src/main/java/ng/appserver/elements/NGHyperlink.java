package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGHyperlink extends NGDynamicGroup {

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

		if( _actionAssociation != null ) {
			System.out.println( context.senderID() );
			href = "/wo/" + "smu"; // Here we're going to need to add the sender ID
		}

		if( href == null ) {
			throw new IllegalStateException( "Failed to generate the href attribute for a hyperlink" );
		}

		response.appendContentString( "<a href=\"" + href + "\">" );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</a>" );
	}
}