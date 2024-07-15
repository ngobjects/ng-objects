package ng.appserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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
	 * FIXME: Shouldn't this really be final and initialized during component construction? // Hugi 2022-01-16
	 */
	private NGComponentDefinition _componentDefinition;

	/**
	 * Stores a reference to the component's parent component
	 */
	private NGComponent _parent;

	/**
	 * Map of this component's children
	 */
	private final Map<String, NGComponent> _children;

	/**
	 * The associations passed in to this component from it's parent component
	 */
	private Map<String, NGAssociation> _associations;

	/**
	 * Reference to the template wrapped by this component (i.e. subcomponent(s) or dynamic element(s))
	 *
	 * As in:
	 * <ThisComponent>[_contentElement]</ThisComponent>
	 */
	private NGElement _contentElement;

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
	 * Invoked before each of the three R-R phases in NGComponent.
	 * Iterates through all the component's bindings, pulls values from the parent component and sets them using KVC
	 */
	public void pullBindingValuesFromParent() {
		if( parent() != null ) {
			if( synchronizesVariablesWithBindings() ) {
				for( final Entry<String, NGAssociation> binding : _associations.entrySet() ) {
					final String bindingName = binding.getKey();
					final NGAssociation association = binding.getValue();
					NGKeyValueCoding.DefaultImplementation.takeValueForKey( this, association.valueInComponent( parent() ), bindingName );
				}
			}
		}
	}

	public void pushBindingValuesToParent() {
		if( parent() != null ) {
			if( synchronizesVariablesWithBindings() ) {
				for( final Entry<String, NGAssociation> binding : _associations.entrySet() ) {
					final String bindingName = binding.getKey();
					final NGAssociation association = binding.getValue();
					association.setValue( NGKeyValueCoding.DefaultImplementation.valueForKey( this, bindingName ), parent() );
				}
			}
		}
	}

	/**
	 * Set the component's context
	 */
	private void setContext( NGContext newContext ) {
		_context = newContext;
	}

	/**
	 * @return The component's context
	 */
	public NGContext context() {
		return _context;
	}

	/**
	 * Sets the context for this component and it's children.
	 *
	 * CHECKME: The name of this method is really just a relic from WO. We don't have the concepts of awake/sleep. So this could really just be… setContext? I mean, when would you really want your component's kids to be in a different context?
	 */
	public void awakeInContext( NGContext newContext ) {
		setContext( newContext );

		for( NGComponent child : _children.values() ) {
			child.awakeInContext( newContext );
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

	public void setValueForBinding( Object value, String bindingName ) {

		final NGAssociation association = _associations.get( bindingName );

		// CHECKME: Should we throw if the binding is not bound here? After all, an explicit operation has failed and should not do so silently // Hugi 2023-03-12
		if( association != null ) {
			association.setValue( value, parent() );
		}
	}

	/**
	 * @return The component's parent component in it's current context
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

	@Override
	public NGResponse generateResponse() {
		context().setPage( this );
		context().setComponent( this );

		// FIXME: We should only be constructing an AjaxResponse if the request is an AjaxRequest
		final NGResponse response = new NGResponse();
		response.setHeader( "content-type", "text/html;charset=utf-8" ); // FIXME: This is most definitely not the place to set the encoding // Hugi 2023-03-12

		// At this point, the context's elementID might be off.
		// For example, if we ended up here by clicking/activating a hyperlink in invokeAction, we'll be in the middle of that component's elementID tree)
		// So we have to start out clean and reset the elementID before entering our forced appendToResponse() stage.
		context()._resetElementID();

		appendToResponse( response, context() );

		// If we have a session, we're going to have to assume our page instance has to be saved
		// Actually, we should only have to save the page instance if we're currently in some way involved in component actions
		// (i.e. the page was a result of a component action invocation, or generates some stateful URLs that reference it)
		// But we don't currently have a way to check for that. So hasSession() it is.
		if( context().hasSession() ) {
			context().session().savePage( context().contextID(), this );
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
		template().appendToResponse( response, context );
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