package ng.appserver.templating.elements;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.templating.NGBindingConfigurationException;
import ng.appserver.templating.NGComponent;
import ng.appserver.templating.NGDynamicElement;
import ng.appserver.templating.NGElement;
import ng.appserver.templating.NGElementManager;
import ng.appserver.templating.NGStructuralElement;
import ng.appserver.templating.assications.NGAssociation;

/**
 * Implementation based on Project Wonder's ERXWOSwitchComponent by ak
 */

public class NGSwitchComponent extends NGDynamicElement implements NGStructuralElement {

	/**
	 * Component name
	 */
	private NGAssociation _componentNameAssociation;

	/**
	 * Associations that will be passed on to the given template
	 */
	private final Map<String, NGAssociation> _componentAssociations;

	/**
	 * The component's wrapped content
	 */
	private final NGElement _contentTemplate;

	private final Map<String, NGElement> _componentCache;
	private final Map<String, String> _elementIDByName;

	public NGSwitchComponent( final String name, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		super( null, null, null );

		_componentNameAssociation = associations.remove( "componentName" );

		if( _componentNameAssociation == null ) {

			// CHECKME: Old binding name from WO, to eventually be removed (see our notes on binding deprecation) // Hugi 2025-05-13
			_componentNameAssociation = associations.remove( "WOComponentName" );

			if( _componentNameAssociation == null ) {
				throw new NGBindingConfigurationException( "[componentName] is a required binding" );
			}
		}

		// Keep the remaining associations for passing on to the component
		_componentAssociations = new HashMap<>( associations );
		_contentTemplate = contentTemplate;
		_componentCache = new ConcurrentHashMap<>();
		_elementIDByName = new ConcurrentHashMap<>();
	}

	private String componentName( final NGComponent localWOComponent ) {
		return (String)_componentNameAssociation.valueInComponent( localWOComponent );
	}

	private String elementNameInContext( String name, final NGContext context ) {
		String id = _elementIDByName.get( name );

		if( id == null ) {
			id = _elementIDByName.size() + "";
			_elementIDByName.put( name, id );
		}

		name = id;

		return name;
	}

	private NGElement realComponentWithName( final String name, final String elementIDString ) {

		// Check if we've already rendered the given component, in that case get the instance.
		NGElement elementInstance = _componentCache.get( elementIDString );

		// No component instance found so we're going to have to construct a new one
		if( elementInstance == null ) {
			elementInstance = NGApplication.application().elementManager().dynamicElementWithName( NGElementManager.GLOBAL_UNNAMESPACED_NAMESPACE, name, _componentAssociations, _contentTemplate );

			if( elementInstance == null ) {
				throw new RuntimeException( "%s  : cannot find component or dynamic element named %s".formatted( getClass().getName(), name ) );
			}

			_componentCache.put( elementIDString, elementInstance );
		}

		return elementInstance;
	}

	@Override
	public void takeValuesFromRequest( final NGRequest request, final NGContext context ) {
		final String name = componentName( context.component() );
		final String id = elementNameInContext( name, context );

		context.elementID().addBranchAndSet( Integer.parseInt( id ) );

		final NGElement componentElement = realComponentWithName( name, id );

		componentElement.takeValuesFromRequest( request, context );

		context.elementID().removeBranch();
	}

	@Override
	public NGActionResults invokeAction( final NGRequest request, final NGContext context ) {
		final String name = componentName( context.component() );
		final String id = elementNameInContext( name, context );

		context.elementID().addBranchAndSet( Integer.parseInt( id ) );

		final NGElement componentElement = realComponentWithName( name, id );

		final NGActionResults localWOActionResults = componentElement.invokeAction( request, context );

		context.elementID().removeBranch();

		return localWOActionResults;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		appendStructureToResponse( response, context );
	}

	@Override
	public void appendStructureToResponse( NGResponse response, NGContext context ) {
		final String name = componentName( context.component() );
		final String id = elementNameInContext( name, context );

		context.elementID().addBranchAndSet( Integer.parseInt( id ) );

		final NGElement componentElement = realComponentWithName( name, id );

		componentElement.appendOrTraverse( response, context );

		context.elementID().removeBranch();
	}
}