package ng.appserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.kvc.NGKeyValueCoding;

/**
 * A component, for use in templates. Basically a user friendly NGElement with an assigned template files(s)
 */

public class NGComponent implements NGElement, NGActionResults {

	private static final Logger logger = LoggerFactory.getLogger( NGComponent.class );

	/**
	 * The page's context
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
	 *
	 * FIXME: Should we be initializing this here? // Hugi 2022-12-30
	 */
	private Map<NGElementID, NGComponent> _children = new HashMap<>();

	/**
	 * Store a reference to the associations
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
	}

	/**
	 * @return The current session
	 *
	 * FIXME: Type safety (for our own session class) would be nice without subclassing in the consuming project. Not sure that's quite achievable here though // Hugi 2023-01-08
	 */
	public NGSession session() {
		return context().session();
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
	public void addChild( NGElementID elementID, NGComponent child ) {
		Objects.requireNonNull( elementID );
		Objects.requireNonNull( child );
		_children.put( elementID, child );
	}

	/**
	 * @return The child with the given elementID. Null if none
	 *
	 * FIXME: Null? Really? // Hugi 2022-12-30
	 */
	public NGComponent getChild( NGElementID elementID ) {
		Objects.requireNonNull( elementID );
		return _children.get( elementID );
	}

	/**
	 * Invoked before each of the three R-R phases in NGComponent.
	 * Iterates through all the component's bindings, pulls values from the parent component and sets them using KVC
	 */
	public void pullBindingValuesfromParent() {
		if( synchronizesVariablesWithBindings() ) {
			for( final Entry<String, NGAssociation> binding : _associations.entrySet() ) {
				final String bindingName = binding.getKey();
				final NGAssociation association = binding.getValue();
				NGKeyValueCoding.DefaultImplementation.takeValueForKey( this, association.valueInComponent( parent() ), bindingName );
			}
		}
	}

	public void pushBindingValuesToParent() {
		if( synchronizesVariablesWithBindings() ) {
			for( final Entry<String, NGAssociation> binding : _associations.entrySet() ) {
				final String bindingName = binding.getKey();
				final NGAssociation association = binding.getValue();
				association.setValue( NGKeyValueCoding.DefaultImplementation.valueForKey( this, bindingName ), parent() );
			}
		}
	}

	/**
	 * FIXME: Working on component actions, exposing this meanwhile. We have to  move the page to a new context when it's restored from the cache
	 */
	private void setContext( NGContext newContext ) {
		_context = newContext;
	}

	public NGContext context() {
		return _context;
	}

	/**
	 * FIXME: I'm keeping in line with familiar names from WO here. We don't have any concept of "awake()" though. Although that's starting to look good...
	 *
	 * Sets the context for this component and it's children
	 */
	public void awakeInContext( NGContext newContext ) {
		setContext( newContext );

		for( NGComponent child : _children.values() ) {
			child.awakeInContext( newContext );
		}
	}

	public <E extends NGComponent> E pageWithName( Class<E> pageClass ) {
		return NGApplication.application().pageWithName( pageClass, context() );
	}

	/**
	 * @return The value of the named binding/association.
	 */
	public Object valueForBinding( String bindingName ) {

		// FIXME: Not a fan of nulls
		//		if( _associations == null ) {
		//			return null;
		//		}

		// Access our associations and fetch the value based on the binding name
		final NGAssociation association = _associations.get( bindingName );

		// A null association means it's not bound, so we're going to return null
		if( association == null ) {
			return null;
		}

		// Now let's go into the parent component and get that value.
		return association.valueInComponent( parent() );
	}

	public NGComponent parent() {
		return _parent;
	}

	/**
	 * FIXME: I feel this should be private, since it's something only the framework should do (during the appendToResponse phase)
	 */
	public void setParent( final NGComponent parent ) {
		_parent = parent;
	}

	/**
	 * FIXME: I feel this should be private, since it's something only the framework should do (during the appendToResponse phase)
	 */
	public void setAssociations( final Map<String, NGAssociation> associations ) {
		_associations = associations;
	}

	/**
	 * FIXME: I feel this should be private, since it's something only the framework should do (during the appendToResponse phase)
	 */
	public NGElement contentElement() {
		return _contentElement;
	}

	/**
	 * FIXME: I feel this should be private, since it's something only the framework should do (during the appendToResponse phase)
	 */
	public void setContentElement( final NGElement contentElement ) {
		_contentElement = contentElement;
	}

	@Override
	public NGResponse generateResponse() {
		logger.debug( "Invoked {}.generateResponse()", getClass() );

		context().setPage( this );
		context().setCurrentComponent( this );

		final NGResponse response = new NGResponse();
		response.setHeader( "content-type", "text/html;charset=utf-8" ); // FIXME: This is most definitely not the place to set the encoding

		appendToResponse( response, context() );

		// FIXME: This will always create a session on pageWithName (which we definitely do not want).
		// We somehow need to check if any elements on the page are component elements that require the page to be stored. // Hugi 2023-01-07
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
		return _componentDefinition.template();
	}

	/**
	 * Sets the component definition for this component instance. See comment on variable, regarding if this method should be private.
	 */
	public void _setComponentDefinition( final NGComponentDefinition componentDefinition ) {
		Objects.requireNonNull( componentDefinition );
		_componentDefinition = componentDefinition;
	}
}