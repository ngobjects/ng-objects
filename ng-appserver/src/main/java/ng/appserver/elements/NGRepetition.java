package ng.appserver.elements;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGBindingConfigurationException;
import ng.appserver.templating.assications.NGAssociation;

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

		if( _countAssociation != null ) {
			final int iterations = count( context );

			for( int i = 0; i < iterations; ++i ) {
				beforeEach( context, i );
				takeValuesFromRequest( request, context );
			}
		}

		if( _listAssociation != null ) {
			final List<?> list = list( context );

			if( list != null ) {
				final int iterations = list.size();

				for( int i = 0; i < iterations; ++i ) {
					beforeEach( context, i );
					final Object object = list.get( i );
					_itemAssociation.setValue( object, context.component() );
					takeChildrenValuesFromRequest( request, context );
				}
			}
		}

		afterAll( context );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		beforeAll( context );

		NGActionResults actionResults = null;

		if( _countAssociation != null ) {
			// CHECKME: The value of the count variable might actually change during the operation if bound to, for example, the size of a list that's manipulated in an invoked action
			// If we want to support this, we have to handle it (same way we do as if [list] is bound, rather than [count]
			final int iterations = count( context );

			for( int i = 0; i < iterations; ++i ) {
				beforeEach( context, i );
				actionResults = invokeChildrenAction( request, context );
			}
		}

		if( _listAssociation != null ) {
			final List<?> list = list( context );

			// Note: You'd think it would be a good idea to cache the list size, right?
			//
			// final int listSize = list.size(); <-- don't do this
			//
			// No. If the invoked action modifies the list, for example by removing an item
			// "listSize" becomes obsolete and we might get an OutOfBoundsException.
			// This is just a warning for future coders.
			//
			// CHECKME: Can't we actually skip the rest of the loop if invokeAction() on a child actually returns a value?

			if( list != null ) {
				for( int i = 0; i < list.size() && actionResults == null; ++i ) {
					beforeEach( context, i );
					final Object object = list.get( i );
					_itemAssociation.setValue( object, context.component() );
					actionResults = invokeChildrenAction( request, context );
				}
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
			final int iterations = count( context );

			for( int i = 0; i < iterations; ++i ) {
				beforeEach( context, i );
				appendChildrenToResponse( response, context );
			}
		}

		if( _listAssociation != null ) {
			final List<?> list = list( context );

			if( list != null ) {
				int iterations = list.size();

				for( int i = 0; i < iterations; ++i ) {
					beforeEach( context, i );
					final Object object = list.get( i );
					_itemAssociation.setValue( object, context.component() );
					appendChildrenToResponse( response, context );
				}
			}
		}

		afterAll( context );
	}

	/**
	 * @return The value of the [count] binding.
	 */
	private int count( final NGContext context ) {
		final Object count = _countAssociation.valueInComponent( context.component() );

		// If the count gets passed in as a numeric constant from the template, it will be a String.
		// We currently have to handle that for each case, although at some point we might want to be more clever about numeric bindings? not really seeing it happening though...
		if( count instanceof String str ) {
			Integer.parseInt( str );
		}

		return (Integer)count;
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
			// CHECKME: Ideally we don't want to convert the sequenced collection to a new List. However, making it redundant requires us to rethink/redesign how we're currently iterate over the list (by index) so here we are // Hugi 2024-09-30
			return List.of( sc.toArray() );
		}

		throw new IllegalArgumentException( "NGRepetition only accepts java.util.List and java Arrays. You sent me a %s".formatted( listAssociationValue.getClass() ) );
	}
}