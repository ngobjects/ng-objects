package ng.appserver.elements;

import java.util.List;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

/**
 * Missing [index]
 */

public class NGRepetition extends NGDynamicGroup {

	/**
	 * The number of iterations to do
	 */
	private final NGAssociation _count;

	/**
	 * The object that will take on the value from [list] during each iteration
	 */
	private final NGAssociation _item;

	/**
	 * List of objects to iterate over
	 */
	private final NGAssociation _list;

	/**
	 * Hold the number of current iteration (zero-based)
	 */
	private final NGAssociation _index;

	public NGRepetition( String _name, Map<String, NGAssociation> associations, NGElement element ) {
		super( _name, associations, element );
		_count = associations.get( "count" );
		_item = associations.get( "item" );
		_index = associations.get( "index" );
		_list = associations.get( "list" );
	}

	/**
	 * FIXME: This needs to be implemented
	 */
	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		super.takeValuesFromRequest( request, context );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		context.elementID().addBranch();

		NGActionResults result = null;

		for( Object object : (List<?>)_list.valueInComponent( context.component() ) ) {
			context.elementID().increment();
			_item.setValue( object, context.component() );
			result = super.invokeAction( request, context );
		}

		context.elementID().removeBranch();

		return result;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		context.elementID().addBranch();

		if( _count != null ) {
			final int count = Integer.parseInt( (String)_count.valueInComponent( context.component() ) );

			for( int i = 0; i < count; ++i ) {
				context.elementID().increment();

				// If an index binding is present, set and increment
				if( _index != null ) {
					_index.setValue( i++, context.component() );
				}

				appendChildrenToResponse( response, context );
			}
		}

		if( _list != null ) {
			final List<?> list = (List<?>)_list.valueInComponent( context.component() );

			int i = 0;

			for( Object object : list ) {
				context.elementID().increment();
				_item.setValue( object, context.component() );

				// If an index binding is present, set and increment
				if( _index != null ) {
					_index.setValue( i++, context.component() );
				}

				appendChildrenToResponse( response, context );
			}
		}

		context.elementID().removeBranch();
	}
}