package ng.appserver.elements;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

/**
 * FIXME: Add support for arrays // Hugi 2022-07-11
 */

public class NGRepetition extends NGDynamicGroup {

	/**
	 * The number of iterations to do
	 */
	private final NGAssociation _countAssociation;

	/**
	 * The object that will take on the value from [list] during each iteration
	 */
	private final NGAssociation _itemAssociation;

	/**
	 * List of objects to iterate over
	 */
	private final NGAssociation _listAssociation;

	/**
	 * Hold the number of current iteration (zero-based)
	 */
	private final NGAssociation _indexAssociation;

	public NGRepetition( String _name, Map<String, NGAssociation> associations, NGElement element ) {
		super( _name, associations, element );
		_countAssociation = associations.get( "count" );
		_itemAssociation = associations.get( "item" );
		_indexAssociation = associations.get( "index" );
		_listAssociation = associations.get( "list" );
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

		NGActionResults actionResults = null;

		List<?> list = (List<?>)_listAssociation.valueInComponent( context.component() );

		// FIXME: Should we be lenient and handle null as an empty list? // Hugi 2023-01-08
		if( list == null ) {
			list = Collections.emptyList();
		}

		final int count = list.size();

		for( int i = 0; i < count && actionResults == null; ++i ) {
			context.elementID().increment(); // FIXME: Better to increment afterwards? // Hugi 2023-01-07
			final Object object = list.get( i );
			_itemAssociation.setValue( object, context.component() );
			actionResults = super.invokeAction( request, context );
		}

		context.elementID().removeBranch();

		return actionResults;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		context.elementID().addBranch();

		if( _countAssociation != null ) {
			final int count = Integer.parseInt( (String)_countAssociation.valueInComponent( context.component() ) );

			for( int i = 0; i < count; ++i ) {
				context.elementID().increment();

				// If an index binding is present, set and increment
				if( _indexAssociation != null ) {
					_indexAssociation.setValue( i++, context.component() );
				}

				appendChildrenToResponse( response, context );
			}
		}

		if( _listAssociation != null ) {
			final List<?> list = (List<?>)_listAssociation.valueInComponent( context.component() );

			int i = 0;

			if( list != null ) {
				for( Object object : list ) {
					context.elementID().increment();
					_itemAssociation.setValue( object, context.component() );

					// If an index binding is present, set and increment
					if( _indexAssociation != null ) {
						_indexAssociation.setValue( i++, context.component() );
					}

					appendChildrenToResponse( response, context );
				}
			}
		}

		context.elementID().removeBranch();
	}
}