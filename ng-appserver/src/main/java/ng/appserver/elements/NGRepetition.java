package ng.appserver.elements;

import java.util.Arrays;
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
 * FIXME: Only partially implemented // Hugi 2023-05-01
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

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		context.elementID().addBranch();

		final List<?> list = list( context );

		final int count = list.size();

		for( int i = 0; i < count; ++i ) {
			context.elementID().increment();
			final Object object = list.get( i );
			_itemAssociation.setValue( object, context.component() );
			super.takeValuesFromRequest( request, context );
		}

		context.elementID().removeBranch();
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		context.elementID().addBranch();

		NGActionResults actionResults = null;

		final List<?> list = list( context );

		final int count = list.size();

		for( int i = 0; i < count && actionResults == null; ++i ) {
			context.elementID().increment();
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
			final List<?> list = list( context );

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

	/**
	 * @return The value passed to the list association coerced to a List (if possible/supported)
	 */
	private List<?> list( final NGContext context ) {
		final Object listAssociationValue = _listAssociation.valueInComponent( context.component() );

		if( listAssociationValue == null ) {
			return Collections.emptyList();
		}

		if( listAssociationValue instanceof List<?> cast ) {
			return cast;
		}

		if( listAssociationValue instanceof Object[] cast ) {
			return Arrays.asList( cast );
		}

		throw new IllegalArgumentException( "NGRepetition only accepts java.util.List and java Arrays. You sent me a %s".formatted( listAssociationValue.getClass() ) );
	}
}