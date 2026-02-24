package ng.appserver.templating.elements.ajax;

import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.associations.NGAssociation;
import ng.appserver.templating.elements.NGDynamicGroup;

/**
 * Observes input field(s) for changes and triggers a server-side action and/or update container refresh.
 *
 * Operates in two modes depending on whether observeFieldID is bound:
 *
 * 1. Single-field mode (observeFieldID is bound):
 *    Renders a hidden <span> marker. The JS binds an 'input' listener to the specified field.
 *
 * 2. Container mode (observeFieldID is NOT bound):
 *    Wraps its children in an element (default <div>, configurable via elementName).
 *    The JS automatically discovers and observes all <input>, <textarea> and <select>
 *    descendants within the container.
 *
 * Bindings:
 *   observeFieldID   - (optional) HTML id of a single input/textarea/select to observe.
 *                       If omitted, the element operates in container mode.
 *   updateContainerID - (required) ID of the AjaxUpdateContainer(s) to update (semicolon-separated for multiple)
 *   action            - (optional) Server-side action to invoke when a field changes
 *   fullSubmit        - (optional) If true, submit all fields from the containing form, not just the changed field.
 *   debounce          - (optional) Debounce delay in milliseconds. The request fires after the user stops
 *                       typing for this duration. Default: 300. Set to 0 for immediate firing on every change.
 *   elementName       - (optional) HTML element name for container mode wrapper (default: "div")
 *   id                - (optional) Explicit HTML id for the container wrapper. Auto-generated if omitted.
 *   class             - (optional) CSS class for the container wrapper
 *   style             - (optional) CSS style for the container wrapper
 */

public class AjaxObserveField extends NGDynamicGroup {

	private final NGAssociation _observeFieldIDAssociation;
	private final NGAssociation _actionAssociation;
	private final NGAssociation _updateContainerIDAssociation;
	private final NGAssociation _fullSubmitAssociation;
	private final NGAssociation _debounceAssociation;
	private final NGAssociation _elementNameAssociation;
	private final NGAssociation _idAssociation;
	private final NGAssociation _classAssociation;
	private final NGAssociation _styleAssociation;

	public AjaxObserveField( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		super( name, associations, contentTemplate );
		_actionAssociation = associations.get( "action" );
		_updateContainerIDAssociation = associations.get( "updateContainerID" );
		_observeFieldIDAssociation = associations.get( "observeFieldID" );
		_fullSubmitAssociation = associations.get( "fullSubmit" );
		_debounceAssociation = associations.get( "debounce" );
		_elementNameAssociation = associations.get( "elementName" );
		_idAssociation = associations.get( "id" );
		_classAssociation = associations.get( "class" );
		_styleAssociation = associations.get( "style" );
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		final String updateContainerID = _updateContainerIDAssociation != null
			? (String)_updateContainerIDAssociation.valueInComponent( context.component() )
			: null;

		final boolean fullSubmit = _fullSubmitAssociation != null
			&& Boolean.TRUE.equals( _fullSubmitAssociation.valueInComponent( context.component() ) );

		if( _observeFieldIDAssociation == null ) {
			// Container mode: wrap children, JS will discover all descendant fields
			final String elementName = _elementNameAssociation != null
				? (String)_elementNameAssociation.valueInComponent( context.component() )
				: "div";

			final String containerID = _idAssociation != null
				? (String)_idAssociation.valueInComponent( context.component() )
				: "ng-observe-" + context.elementID().toString().replace( '.', '_' );

			final StringBuilder tag = new StringBuilder();
			tag.append( "<" ).append( elementName );
			tag.append( " id=\"" ).append( containerID ).append( "\"" );
			tag.append( " data-observedescendants=\"true\"" );

			if( updateContainerID != null ) {
				tag.append( " data-updatecontainerid=\"" ).append( updateContainerID ).append( "\"" );
			}

			tag.append( " data-action=\"" ).append( context.componentActionURL() ).append( "\"" );
			tag.append( " data-fullsubmit=\"" ).append( fullSubmit ).append( "\"" );

			if( _debounceAssociation != null ) {
				final Object debounce = _debounceAssociation.valueInComponent( context.component() );
				if( debounce != null ) {
					tag.append( " data-debounce=\"" ).append( debounce ).append( "\"" );
				}
			}

			if( _classAssociation != null ) {
				final String cssClass = (String)_classAssociation.valueInComponent( context.component() );
				if( cssClass != null ) {
					tag.append( " class=\"" ).append( cssClass ).append( "\"" );
				}
			}

			if( _styleAssociation != null ) {
				final String style = (String)_styleAssociation.valueInComponent( context.component() );
				if( style != null ) {
					tag.append( " style=\"" ).append( style ).append( "\"" );
				}
			}

			tag.append( ">" );
			response.appendContentString( tag.toString() );

			appendChildrenToResponse( response, context );

			response.appendContentString( "</" + elementName + ">" );
		}
		else {
			// Single-field mode: hidden span marker
			final String observeFieldID = (String)_observeFieldIDAssociation.valueInComponent( context.component() );

			final StringBuilder html = new StringBuilder();
			html.append( "<span style=\"display:none\"" );
			html.append( " data-observefieldid=\"" ).append( observeFieldID ).append( "\"" );

			if( updateContainerID != null ) {
				html.append( " data-updatecontainerid=\"" ).append( updateContainerID ).append( "\"" );
			}

			html.append( " data-action=\"" ).append( context.componentActionURL() ).append( "\"" );
			html.append( " data-fullsubmit=\"" ).append( fullSubmit ).append( "\"" );

			if( _debounceAssociation != null ) {
				final Object debounce = _debounceAssociation.valueInComponent( context.component() );
				if( debounce != null ) {
					html.append( " data-debounce=\"" ).append( debounce ).append( "\"" );
				}
			}

			html.append( "></span>" );

			response.appendContentString( html.toString() );
		}
	}

	@Override
	public NGActionResults invokeAction( final NGRequest request, final NGContext context ) {

		// Check if this element itself is the sender (action invoked by the observe field)
		if( context.elementID().equals( context.senderID() ) ) {
			if( _actionAssociation != null ) {
				return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
			}
		}

		// Delegate to children (NGDynamicGroup handles this; no-op if no children)
		return invokeChildrenAction( request, context );
	}
}
