package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;
import ng.appserver.templating.NGBindingConfigurationException;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;
import ng.appserver.templating.assications.NGAssociationUtils;

public class NGHyperlink extends NGDynamicGroup {

	private final NGAssociation _hrefAssociation;
	private final NGAssociation _actionAssociation;
	private final NGAssociation _pageNameAssociation;

	/**
	 * If disabled is set to true, the link will not be rendered (only it's content will be) and it won't perform invokeAction even if it's elementID is targeted
	 */
	private final NGAssociation _disabledAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private final Map<String, NGAssociation> _additionalAssociations;

	public NGHyperlink( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );

		// Clone the dictionary so additionalAssociations will contain additional associations
		// (bindings that get passed directly to the <a> tag as attributes.
		_additionalAssociations = new HashMap<>( associations );
		_hrefAssociation = _additionalAssociations.remove( "href" );
		_actionAssociation = _additionalAssociations.remove( "action" );
		_pageNameAssociation = _additionalAssociations.remove( "pageName" );
		_disabledAssociation = _additionalAssociations.remove( "disabled" );

		if( _hrefAssociation == null && _actionAssociation == null && _pageNameAssociation == null ) {
			throw new NGBindingConfigurationException( "You must bind one of [action], [pageName] or [href]" );
		}
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		// If the link is disabled, we render only it's children (and none of the link itself)
		if( isDisabled( context ) ) {
			appendChildrenToResponse( response, context );
		}
		else {
			String href = null;

			if( _hrefAssociation != null ) {
				href = (String)_hrefAssociation.valueInComponent( context.component() );
			}
			else if( _actionAssociation != null || _pageNameAssociation != null ) {
				href = context.componentActionURL();
			}

			final Map<String, String> attributes = new HashMap<>();

			if( href != null ) {
				attributes.put( "href", href );
			}

			_additionalAssociations.forEach( ( name, ass ) -> {
				final Object value = ass.valueInComponent( context.component() );

				if( value != null ) {
					attributes.put( name, value.toString() );
				}
			} );

			response.appendContentString( NGHTMLUtilities.createElementStringWithAttributes( "a", attributes, false ) );
			appendChildrenToResponse( response, context );
			response.appendContentString( "</a>" );
		}
	}

	@Override
	public NGActionResults invokeAction( final NGRequest request, final NGContext context ) {

		if( context.currentElementIsSender() ) {
			// Don't respond to action invocations if the link is disabled
			if( !isDisabled( context ) ) {
				if( _actionAssociation != null ) {
					return (NGActionResults)_actionAssociation.valueInComponent( context.component() );
				}

				if( _pageNameAssociation != null ) {
					final String pageName = (String)_pageNameAssociation.valueInComponent( context.component() );
					final NGComponent actionResults = NGApplication.application().pageWithName( pageName, context );
					return actionResults;
				}
			}
		}

		return null;
	}

	/**
	 * @return true if the "disabled" value evaluates to truth
	 */
	private boolean isDisabled( NGContext context ) {
		return _disabledAssociation != null && NGAssociationUtils.isTruthy( _disabledAssociation.valueInComponent( context.component() ) );
	}
}