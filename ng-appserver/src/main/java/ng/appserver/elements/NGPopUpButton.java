package ng.appserver.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.privates._NGUtilities;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;

/**
 * FIXME: Add support for <optgroup> // Hugi 2024-07-13
 */

public class NGPopUpButton extends NGDynamicElement {

	private static final String NO_SELECTION_OPTION_VALUE = "NO_SELECTION_OPTION_VALUE";

	private final NGAssociation _listAss;

	private final NGAssociation _itemAss;

	private final NGAssociation _displayStringAss;

	private final NGAssociation _noSelectionStringAss;

	private final NGAssociation _selectionAss;

	private final NGAssociation _disabledAss;

	private final NGAssociation _nameAss;

	private final NGAssociation _indexAss;

	/**
	 * FIXME: We might want to offer the opportunity to replace the entire "selections" association in the target component, instead of modifying the existing collection // Hugi 2024-07-13
	 */
	private final NGAssociation _selectionsAss;

	private final NGAssociation _multipleAss;

	/**
	 * Pass-through attributes
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGPopUpButton( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_additionalAssociations = new HashMap<>( associations );
		_listAss = _additionalAssociations.remove( "list" );
		_itemAss = _additionalAssociations.remove( "item" );
		_displayStringAss = _additionalAssociations.remove( "displayString" );
		_noSelectionStringAss = _additionalAssociations.remove( "noSelectionString" );
		_selectionAss = _additionalAssociations.remove( "selection" );
		_disabledAss = _additionalAssociations.remove( "disabled" );
		_nameAss = _additionalAssociations.remove( "name" );
		_indexAss = _additionalAssociations.remove( "index" );

		// For multiselect
		_selectionsAss = _additionalAssociations.remove( "selections" );
		_multipleAss = _additionalAssociations.remove( "multiple" );
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		if( !disabled( context ) ) {
			final String name = name( context );
			final List<String> valuesFromRequest = request.formValuesForKey( name );

			// FIXME: We might have to handle probably have to handle "empty" for the multiple case
			if( !valuesFromRequest.isEmpty() ) {
				if( multiple( context ) ) {
					takeMultipleValuesFromRequest( context, name, valuesFromRequest );
				}
				else {
					takeSingleValueFromRequest( context, name, valuesFromRequest );
				}
			}
		}
	}

	private void takeMultipleValuesFromRequest( final NGContext context, final String name, final List<String> valuesFromRequest ) {

		// FIXME: We might want to offer th user the opportunity that we modify the original collection instead of passing in a new one // Hugi 2024-07-13
		final List<?> list = list( context );

		final List selectedItems = new ArrayList<>();

		for( String index : valuesFromRequest ) {
			selectedItems.add( list.get( Integer.parseInt( index ) ) );
		}

		_selectionsAss.setValue( selectedItems, context.component() );
	}

	private void takeSingleValueFromRequest( final NGContext context, final String name, final List<String> valuesFromRequest ) {

		// If multiple form values are present for the same field name, the potential for an error condition is probably high enough to just go ahead and fail.
		if( valuesFromRequest.size() > 1 ) {
			throw new IllegalStateException( "The request contains %s form values named '%s'. I can only handle one at a time. The values you sent me are (%s).".formatted( valuesFromRequest.size(), name, valuesFromRequest ) );
		}

		final String stringValueFromRequest = valuesFromRequest.get( 0 );

		// If nothing is selected, we push null to the selection
		if( NO_SELECTION_OPTION_VALUE.equals( stringValueFromRequest ) ) {
			_selectionAss.setValue( null, context.component() );
		}
		else {
			final int selectionIndex = Integer.parseInt( stringValueFromRequest );
			final List<?> list = list( context );
			final Object selectedItem = list.get( selectionIndex );
			_selectionAss.setValue( selectedItem, context.component() );
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		final Map<String, String> attributes = new HashMap<>();

		attributes.put( "name", name( context ) );

		NGHTMLUtilities.addAssociationValuesToAttributes( attributes, _additionalAssociations, context.component() );

		if( multiple( context ) ) {
			attributes.put( "multiple", "true" );
		}

		if( disabled( context ) ) {
			attributes.put( "disabled", "" );
		}

		final List<?> list = list( context );

		response.appendContentString( NGHTMLUtilities.createElementStringWithAttributes( "select", attributes, false ) );

		if( _noSelectionStringAss != null ) {
			final String noSelectionString = (String)_noSelectionStringAss.valueInComponent( context.component() );
			response.appendContentString( "<option value=\"%s\">%s</option>".formatted( NO_SELECTION_OPTION_VALUE, noSelectionString ) );
		}

		int index = 0;

		for( Object object : list ) {
			// Set the value of the item binding in our current context
			// FIXME: we have to assume that item isn't neccessarily bound // Hugi 2023-05-01
			_itemAss.setValue( object, context.component() );

			// Set the value of the index binding if bound
			if( _indexAss != null ) {
				_indexAss.setValue( index, context.component() );
			}

			final Object displayString = _displayStringAss.valueInComponent( context.component() );

			boolean isSelected = false;

			// FIXME: Hacky way to get the currently selected item. We should be reusing logic from takeValuesFromRequest() here
			if( multiple( context ) ) {
				final List<String> selectedIndexes = context.request().formValuesForKey( name( context ) );
				isSelected = selectedIndexes.contains( String.valueOf( index ) );
			}
			else {
				final String indexValue = context.request().formValueForKey( name( context ) );

				if( indexValue != null && !indexValue.equals( NO_SELECTION_OPTION_VALUE ) && Integer.parseInt( indexValue ) == index ) {
					isSelected = true;
				}

				if( _selectionAss != null ) {
					if( object.equals( _selectionAss.valueInComponent( context.component() ) ) ) {
						isSelected = true;
					}
				}
			}

			final String selectedMarker = isSelected ? " selected" : "";

			// FIXME: We're currently always using the index as "value". We're going to want to allow the passing in of a custom value attribute through the value binding // Hugi 2023-05-01
			response.appendContentString( "<option value=\"%s\"%s>%s</option>".formatted( index, selectedMarker, displayString ) );
			index++;
		}

		response.appendContentString( "</select>" );
	}

	/**
	 * @return True if the field is disabled
	 */
	private boolean disabled( final NGContext context ) {
		if( _disabledAss != null ) {
			return _NGUtilities.isTruthy( _disabledAss.valueInComponent( context.component() ) );
		}

		return false;
	}

	/**
	 * @return True if the field is disabled
	 */
	private boolean multiple( final NGContext context ) {
		if( _multipleAss != null ) {
			return _NGUtilities.isTruthy( _multipleAss.valueInComponent( context.component() ) );
		}

		return false;
	}

	/**
	 * @return The name of the field (to use in the HTML code)
	 */
	private String name( final NGContext context ) {

		if( _nameAss != null ) {
			return (String)_nameAss.valueInComponent( context.component() );
		}

		return context.elementID().toString();
	}

	/**
	 * @return The value passed to the list association coerced to a List (if possible/supported)
	 */
	private List<?> list( final NGContext context ) {
		final Object listAssociationValue = _listAss.valueInComponent( context.component() );

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