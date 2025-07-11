package ng.appserver.templating;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGContext;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.NGResponseMultipart;
import ng.appserver.NGSession;
import ng.appserver.templating.assications.NGAssociation;
import ng.kvc.NGKeyValueCoding;

/**
 * A component, for use in templates. Basically a user friendly NGElement with an assigned template files(s)
 */

public class NGComponent implements NGElement, NGActionResults {

	/**
	 * The component's context
	 */
	private NGContext _context;

	/**
	 * The component definition from which this component is constructed.
	 * FIXME: Shouldn't this really be final and initialized during component construction? // Hugi 2022-01-16
	 */
	private NGComponentDefinition _componentDefinition;

	/**
	 * This component's parent component if any, [null] if it's the root component
	 */
	private NGComponent _parent;

	/**
	 * Map of this component's children, with their elementID in the template as key
	 */
	private final Map<String, NGComponent> _children;

	/**
	 * Associations bound to this component from it's parent component
	 */
	private Map<String, NGAssociation> _associations;

	/**
	 * Template wrapped by this component (i.e. subcomponent(s) or dynamic element(s))
	 *
	 * As in:
	 * <ThisComponent>[_contentElement]</ThisComponent>
	 */
	private NGElement _contentElement;

	/**
	 * The default component constructor
	 *
	 *  CHECKME:
	 *  I'm not sure this constructor should exist.
	 *  Manual construction of components should never be done by the user - and the actual constructor (that should be protected) really needs to take a component definition as an argument.
	 *  The only reason it is kept around is becuse it makes migration of older codebases easier (where this constructor is in every component).
	 *  // Hugi 2025-04-16
	 */
	public NGComponent( final NGContext context ) {
		Objects.requireNonNull( context );
		_context = context;
		_children = new HashMap<>();
	}

	/**
	 * @return The component's name, as delivered by the component's definition
	 */
	public String name() {
		return _componentDefinition.name();
	}

	/**
	 * @return The current session
	 */
	public NGSession session() {
		return context().session();
	}

	/**
	 * @return The application within which this component instance was constructed
	 */
	public NGApplication application() {
		return NGApplication.application();
	}

	/**
	 * @return true if this component should push/pull values to/from it's parent
	 */
	public boolean synchronizesVariablesWithBindings() {
		return true;
	}

	/**
	 * Add the given child component with the given elementID
	 */
	public void addChild( String elementID, NGComponent child ) {
		Objects.requireNonNull( elementID );
		Objects.requireNonNull( child );
		_children.put( elementID, child );
	}

	/**
	 * @return The child with the given elementID. Null if none
	 */
	public NGComponent getChild( String elementID ) {
		Objects.requireNonNull( elementID );
		return _children.get( elementID );
	}

	/**
	 * If the component is synchronized, this method gets invoked before each of the three R-R phases in NGComponent.
	 * Iterates through all of component's bindings, pulls values from it's parent and sets them using KVC.
	 */
	public void pullBindingValuesFromParent() {
		if( parent() != null ) {
			if( synchronizesVariablesWithBindings() ) {
				for( final Entry<String, NGAssociation> binding : _associations.entrySet() ) {
					final String bindingName = binding.getKey();
					final NGAssociation association = binding.getValue();
					final Object value = association.valueInComponent( parent() );
					NGKeyValueCoding.Utility.takeValueForKey( this, value, bindingName );
				}
			}
		}
	}

	/**
	 * If the component is synchronized, this method gets invoked after each of the three R-R phases in NGComponent.
	 * Iterates through all of component's bindings and pushes it's values to it's parent, setting them using KVC.
	 */
	public void pushBindingValuesToParent() {
		if( parent() != null ) {
			if( synchronizesVariablesWithBindings() ) {
				for( final Entry<String, NGAssociation> binding : _associations.entrySet() ) {
					final String bindingName = binding.getKey();
					final NGAssociation association = binding.getValue();
					final Object value = NGKeyValueCoding.Utility.valueForKey( this, bindingName );
					association.setValue( value, parent() );
				}
			}
		}
	}

	/**
	 * @return The component's context
	 */
	public NGContext context() {
		return _context;
	}

	/**
	 * Set the context for this component (not including children)
	 */
	private void setContext( NGContext newContext ) {
		_context = newContext;
	}

	/**
	 * Sets the context for this component and it's children.
	 */
	public void setContextIncludingChildren( NGContext newContext ) {
		setContext( newContext );

		for( NGComponent child : _children.values() ) {
			child.setContextIncludingChildren( newContext );
		}
	}

	/**
	 * @return A new page level component
	 */
	public <E extends NGComponent> E pageWithName( Class<E> pageClass ) {
		return application().pageWithName( pageClass, context() );
	}

	/**
	 * @return true if the named binding is bound
	 */
	public boolean hasBinding( final String bindingName ) {
		return _associations.get( bindingName ) != null;
	}

	/**
	 * @return The value of the named binding/association.
	 */
	public Object valueForBinding( String bindingName ) {

		if( parent() == null ) {
			return null;
		}

		// Access our associations and fetch the value based on the binding name
		final NGAssociation association = _associations.get( bindingName );

		// A null association means it's not bound, so we're going to return null
		// CHECKME: Just returning null for an unbound binding isn't exactly nice. Should we look into failure modes here? // Hugi 2023-07-15
		if( association == null ) {
			return null;
		}

		// Now let's go into the parent component and get that value.
		return association.valueInComponent( parent() );
	}

	/**
	 * Sets the given binding to the given value
	 */
	public void setValueForBinding( Object value, String bindingName ) {

		final NGAssociation association = _associations.get( bindingName );

		// CHECKME: Should we throw if the binding is not bound here? After all, an explicit operation has failed and should not do so silently // Hugi 2023-03-12
		if( association != null ) {
			association.setValue( value, parent() );
		}
	}

	/**
	 * @return The component's parent component in it's current context, null if no parent is present
	 */
	public NGComponent parent() {
		return _parent;
	}

	/**
	 * CHECKME: Kind of feel like this should be private, since it's something only the framework does
	 */
	public void setParent( final NGComponent parent, final Map<String, NGAssociation> associations, final NGElement contentElement ) {
		_parent = parent;
		_associations = associations;
		_contentElement = contentElement;
	}

	/**
	 * The component's contained content in it's current context
	 */
	public NGElement contentElement() {
		return _contentElement;
	}

	/**
	 * Generates the response for this component, setting it as it's context page and storing it in the page cache if required.
	 */
	@Override
	public NGResponse generateResponse() {
		context().setPage( this );
		context().setComponent( this );

		// At this point, the context's elementID might be off.
		// For example, we might have ended up here by clicking/activating a hyperlink in invokeAction in the same context.
		// In that case, the context's elementID will still be somewhere in the middle of that component's element tree.
		//
		// This may also apply if we decide to return a different page in the middle of generating a one,
		// the most obvious example being an exception occurring, which means us switching to generating an exception page.
		//
		// So we have to start out clean and reset the elementID before entering appendToResponse().
		context()._resetElementID();

		// Now let's create a new response and append ourselves to it
		final NGResponse response;

		// FIXME: It feels wrong to determine here the type of response we're constructing, but going with it for testing for now // Hugi 2025-04-13
		if( context().targetsMultipleUpdateContainers() ) {
			response = new NGResponseMultipart( context() );
			response.setHeader( "content-type", "multipart/form-data; boundary=" + NGResponseMultipart.BOUNDARY );
		}
		else {
			response = new NGResponse();
			response.setHeader( "content-type", "text/html;charset=utf-8" ); // FIXME: This is most definitely not the place to set the encoding // Hugi 2023-03-12
		}

		appendToResponse( response, context() );

		// So, we've generated the page, and it's ready to return. Now we let the context tell us whether it should be stored in the page cache for future reference.
		// CHECKME: This *still* feels a little like the wrong place to stash the page in the cache. Not yet sure where it *should* be but my gut *still* has a feeling // Hugi 2024-09-28
		// FIXME: We might be targeting multiple update containers, this isn't properly handled at the moment // Hugi 2025-05-09
		if( context()._shouldSaveInPageCache() ) {
			context().session().pageCache().savePage( context().contextID(), this, context()._originatingContextID(), context().targetedUpdateContainerIDsStringValue() );
		}

		return response;
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		template().takeValuesFromRequest( request, context );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return template().invokeAction( request, context );
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {

		final NGElement template = template();

		// FIXME: I think this actually belongs in NGComponentReference // Hugi 2025-04-15
		if( context.shouldAppendToResponse() ) {
			template.appendToResponse( response, context );
		}
		else if( template instanceof NGStructuralElement se ) {
			se.appendStructureToResponse( response, context );
		}
	}

	/**
	 * @return the template (which is stored by the component definition)
	 */
	public NGElement template() {
		final NGElement template = _componentDefinition.template();

		if( template == null ) {
			throw new IllegalStateException( "The component %s has no template. You must either provide a component template file or override the component's template() method".formatted( name() ) );
		}

		return template;
	}

	/**
	 * Sets the component definition for this component instance.
	 * CHECKME: See comment on variable, regarding if this method should be private // Hugi 2023-07-15
	 */
	public void _setComponentDefinition( final NGComponentDefinition componentDefinition ) {
		Objects.requireNonNull( componentDefinition );
		_componentDefinition = componentDefinition;
	}
}