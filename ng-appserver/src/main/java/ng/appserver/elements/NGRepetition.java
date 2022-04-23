package ng.appserver.elements;

import java.util.List;
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

	public NGRepetition( String _name, Map<String, NGAssociation> associations, List<NGElement> children ) {
		super( _name, associations, children );
		_count = associations.get( "count" );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final int count = (Integer)_count.valueInComponent( context.component() );

		for( int i = 0; i < count; ++i ) {
			appendChildrenToResponse( response, context );
		}
	}
}