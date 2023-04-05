package ng.appserver.elements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGBindingConfigurationException;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

/**
 * Implementation based on Project Wonder's ERXWOSwitchComponent by
 *
 * Original author from Project Wonder: ak
 */

public class NGSwitchComponent extends NGDynamicElement {

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

	public NGSwitchComponent( final String paramString, final Map<String, NGAssociation> associations, final NGElement contentTemplate ) {
		super( null, null, null );

		_componentNameAssociation = associations.get( "componentName" );

		if( _componentNameAssociation == null ) {
			throw new NGBindingConfigurationException( "[componentName] is a reuired binding" );
		}

		_componentAssociations = new HashMap<>( associations );
		_componentAssociations.remove( "componentName" );

		_componentCache = new ConcurrentHashMap<>();
		_contentTemplate = contentTemplate;

		_elementIDByName = new ConcurrentHashMap<>();
	}

	private String componentName( final NGComponent localWOComponent ) {
		return (String)_componentNameAssociation.valueInComponent( localWOComponent );
	}

	public String _elementNameInContext( String name, final NGContext paramWOContext ) {
		String id = _elementIDByName.get( name );

		if( id == null ) {
			id = _elementIDByName.size() + "";
			_elementIDByName.put( name, id );
		}

		name = id;

		return name;
	}

	public NGElement _realComponentWithName( final String name, final String elementIDString ) {

		// Check if we've already rendered the given component, in that case get the instance.
		NGElement localWOElement = _componentCache.get( elementIDString );

		// No component instance found so we're going to have to construct a new one
		if( localWOElement == null ) {
			localWOElement = NGApplication.application().dynamicElementWithName( name, _componentAssociations, _contentTemplate, Collections.emptyList() );

			if( localWOElement == null ) {
				throw new RuntimeException( "<" + getClass().getName() + "> : cannot find component or dynamic element named " + name );
			}

			_componentCache.put( elementIDString, localWOElement );
		}

		return localWOElement;
	}

	@Override
	public void takeValuesFromRequest( final NGRequest request, final NGContext context ) {
		final String name = componentName( context.component() );
		final String id = _elementNameInContext( name, context );

		context.elementID().addBranchAndSet( Integer.parseInt( id ) );

		final NGElement componentElement = _realComponentWithName( name, id );

		componentElement.takeValuesFromRequest( request, context );

		context.elementID().removeBranch();
	}

	@Override
	public NGActionResults invokeAction( final NGRequest request, final NGContext context ) {
		final String name = componentName( context.component() );
		final String id = _elementNameInContext( name, context );

		context.elementID().addBranchAndSet( Integer.parseInt( id ) );

		final NGElement componentElement = _realComponentWithName( name, id );

		final NGActionResults localWOActionResults = componentElement.invokeAction( request, context );

		context.elementID().removeBranch();

		return localWOActionResults;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final String name = componentName( context.component() );
		final String id = _elementNameInContext( name, context );

		context.elementID().addBranchAndSet( Integer.parseInt( id ) );

		final NGElement componentElement = _realComponentWithName( name, id );

		componentElement.appendToResponse( response, context );

		context.elementID().removeBranch();
	}
}