package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGHyperlink extends NGDynamicGroup {

	private final NGAssociation _hrefAssociation;

	public NGHyperlink( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_hrefAssociation = associations.get( "href" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final String href = (String)_hrefAssociation.valueInComponent( context.component() );

		response.appendContentString( "<a href=\"" + href + "\">" );
		appendChildrenToResponse( response, context );
		response.appendContentString( "</a>" );
	}
}