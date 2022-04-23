package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGRepetition extends NGDynamicGroup {

	/**
	 * The number of iterations to do
	 */
	private final NGAssociation _count;

	public NGRepetition( String _name, Map<String, NGAssociation> associations, NGElement element ) {
		super( _name, associations, element );
		_count = associations.get( "count" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final int count = Integer.parseInt( (String)_count.valueInComponent( context.component() ) );

		for( int i = 0; i < count; ++i ) {
			appendChildrenToResponse( response, context );
		}
	}
}