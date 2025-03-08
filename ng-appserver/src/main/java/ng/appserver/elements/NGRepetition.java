package ng.appserver.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;

import ng.appserver.NGActionResults;
import ng.appserver.NGAssociation;
import ng.appserver.NGBindingConfigurationException;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

/**
 * A wrapper element that allows you to:
 *
 *  1. Iterate over a java array/List/SequencedCollection
 *  2. Display it's contained template [count] times
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

		if( _listAssociation == null && _countAssociation == null ) {
			throw new NGBindingConfigurationException( "You must bind either [list] or [count]" );
		}

		if( _listAssociation != null && _countAssociation != null ) {
			throw new NGBindingConfigurationException( "You can't bind both [list] and [count]" );
		}
	}

	/**
	 * Invoked at the start of each R-R phase for preparation
	 */
	private void beforeAll( final NGContext context ) {
		context.elementID().addBranch();
	}

	/**
	 * Invoked at the start of each of the repetition's iterations for preparation
	 */
	private void beforeEach( final NGContext context, final int iterationIndex ) {
		context.elementID().increment();

		// If an index binding is present, set it
		if( _indexAssociation != null ) {
			_indexAssociation.setValue( iterationIndex, context.component() );
		}
	}

	/**
	 * Invoked at the end of each R-R phase for cleanup of bindings and elementID
	 */
	private void afterAll( final NGContext context ) {
		if( _itemAssociation != null ) {
			_itemAssociation.setValue( null, context.component() );
		}

		context.elementID().removeBranch();
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		beforeAll( context );

		if( _listAssociation != null ) {
			final List<?> list = list( context );

			final int count = list.size();

			for( int i = 0; i < count; ++i ) {
				beforeEach( context, i );
				final Object object = list.get( i );
				_itemAssociation.setValue( object, context.component() );
				super.takeValuesFromRequest( request, context );
			}
		}

		afterAll( context );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		beforeAll( context );

		NGActionResults actionResults = null;

		if( _listAssociation != null ) {
			final List<?> list = list( context );

			// Note: You'd think it would be a good idea to cache the list size, right?
			//
			// final int listSize = list.size(); <-- don't do this
			//
			// No. If the invoked action modifies the list, for example by removing an item
			// "listSize" becomes obsolete and we might get an OutOfBoundsException.
			// This is just a warning for future coders.

			for( int i = 0; i < list.size() && actionResults == null; ++i ) {
				beforeEach( context, i );
				final Object object = list.get( i );
				_itemAssociation.setValue( object, context.component() );
				actionResults = super.invokeAction( request, context );
			}
		}

		afterAll( context );

		return actionResults;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		appendStructureToResponse( response, context );
	}

	@Override
	public void appendStructureToResponse( NGResponse response, NGContext context ) {
		beforeAll( context );

		if( _countAssociation != null ) {
			final int count = Integer.parseInt( (String)_countAssociation.valueInComponent( context.component() ) );

			for( int i = 0; i < count; ++i ) {
				beforeEach( context, i );
				appendChildrenToResponse( response, context );
			}
		}

		if( _listAssociation != null ) {
			final List<?> list = list( context );

			int i = 0;

			if( list != null ) {
				for( Object object : list ) {
					beforeEach( context, i++ );
					_itemAssociation.setValue( object, context.component() );
					appendChildrenToResponse( response, context );
				}
			}
		}

		afterAll( context );
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

		if( listAssociationValue instanceof SequencedCollection sc ) {
			// FIXME: This conversion of the sequenced collection to a List shouldn't be required and is a potential performance issue. But making it redundant requires us to rethink/redesign how we're currently getting the list value to iterate over // Hugi 2024-09-30
			final ArrayList<Object> list = new ArrayList<>();
			sc.iterator().forEachRemaining( list::add );
			return list;
		}

		throw new IllegalArgumentException( "NGRepetition only accepts java.util.List and java Arrays. You sent me a %s".formatted( listAssociationValue.getClass() ) );
	}
}