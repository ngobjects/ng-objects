package ng.appserver.templating.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;

import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.associations.NGAssociationUtils;

/**
 * FIXME: Add support for <optgroup> // Hugi 2024-07-13
 * FIXME: Currently always using an item's index in the original 'list' as an option's "value". We're missing a 'value' association to allow a custom value to go through to the generated request parameters // Hugi 2023-05-01
 * FIXME: Currently always replacing the value of the targeted "selections" binding with a new List. Having the option to modify an existing collection might be good.
 */

public class NGPopUpButton extends NGDynamicElement {

	private static final String NO_SELECTION_OPTION_VALUE = "NO_SELECTION_OPTION_VALUE";

	private final NGAssociation _listAss;
	private final NGAssociation _itemAss;
	private final NGAssociation _displayStringAss;
	private final NGAssociation _noSelectionStringAss;
	private final NGAssociation _disabledAss;
	private final NGAssociation _nameAss;
	private final NGAssociation _indexAss;
	private final NGAssociation _multipleAss;
	private final NGAssociation _selectionAss;
	private final NGAssociation _selectionsAss;

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
			final List<String> selectedValuesFromRequest = request.formValuesForKey( name );

			if( multiple( context ) ) {
				takeValuesFromRequestMultiple( context, name, selectedValuesFromRequest );
			}
			else {
				takeValuesFromRequestSingle( context, name, selectedValuesFromRequest );
			}
		}
	}

	private void takeValuesFromRequestMultiple( final NGContext context, final String name, final List<String> valuesFromRequest ) {

		// FIXME: OK, we have something of a problem with NGBrowser.
		// Selecting no objects in the UI results in it not being represented in the request's formValues at all (no value present for it's 'name').
		// This means that in the case of multiple forms on the same page and a form being submitted - we don't actually know if the value should be set to an empty list (we don't know if the containing form was submitted or if it was a different form).
		// The only way I can think of to make this work is if we can check if the actual *form* containing the current browser is being submitted. In that case, we can definitely interpret the absence of a value as "no values selected".
		// The current functionality means that if a browser is in one form on a page and a different form is submitted, the browser will always interpret that as "no values selected".
		// Hugi 2025-08-04
		final List<?> list = list( context );

		final List selectedItems = new ArrayList<>();

		for( String index : valuesFromRequest ) {
			selectedItems.add( list.get( Integer.parseInt( index ) ) );
		}

		_selectionsAss.setValue( selectedItems, context.component() );
	}

	private void takeValuesFromRequestSingle( final NGContext context, final String name, final List<String> valuesFromRequest ) {

		if( !valuesFromRequest.isEmpty() ) {
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

			// Set the value of the item binding if bound
			if( _itemAss != null ) {
				_itemAss.setValue( object, context.component() );
			}

			// Set the value of the index binding if bound
			if( _indexAss != null ) {
				_indexAss.setValue( index, context.component() );
			}

			final Object displayString;

			if( _displayStringAss != null ) {
				displayString = _displayStringAss.valueInComponent( context.component() );
			}
			else {
				final Object current = list.get( index );
				displayString = current != null ? current.toString() : "<null>";
			}

			boolean isSelected = false;

			if( multiple( context ) ) {
				final List selections = (List)_selectionsAss.valueInComponent( context.component() );

				if( selections != null && selections.contains( object ) ) {
					isSelected = true;
				}
			}
			else {
				final String selectedValue = context.request().formValueForKey( name( context ) );

				if( selectedValue != null && !selectedValue.equals( NO_SELECTION_OPTION_VALUE ) && Integer.parseInt( selectedValue ) == index ) {
					isSelected = true;
				}

				if( _selectionAss != null ) {
					final Object selectedObject = _selectionAss.valueInComponent( context.component() );

					if( object.equals( selectedObject ) ) {
						isSelected = true;
					}
				}
			}

			final String selectedMarker = isSelected ? " selected" : "";

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
			return NGAssociationUtils.isTruthy( _disabledAss.valueInComponent( context.component() ) );
		}

		return false;
	}

	/**
	 * @return True if the field is disabled
	 */
	private boolean multiple( final NGContext context ) {
		if( _multipleAss != null ) {
			return NGAssociationUtils.isTruthy( _multipleAss.valueInComponent( context.component() ) );
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

		if( listAssociationValue instanceof SequencedCollection sc ) {
			// CHECKME: Ideally we don't want to convert the sequenced collection to a new List. However, making it redundant requires us to rethink/redesign how we're currently iterate over the list (by index) so here we are // Hugi 2024-09-30
			return List.of( sc.toArray() );
		}

		throw new IllegalArgumentException( "%s only accepts java.util.List, java arrays and java.util.SequencedCollection. You sent me a %s".formatted( getClass().getSimpleName(), listAssociationValue.getClass() ) );
	}
}