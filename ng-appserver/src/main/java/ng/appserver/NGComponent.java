package ng.appserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.kvc.NGKeyValueCoding;

/**
 * FIXME: Should we allow creation of components without a context?
 * FIXME: Templating really belongs in a separate project, right?
 */

public class NGComponent implements NGElement, NGActionResults {

	private final NGContext _context;

	/**
	 * FIXME: Shouldn't this really be final and initialized during component construction? // Hugi 2022-01-16
	 * FIXME: This should definitely not be public
	 */
	public NGComponentDefinition _componentDefinition;

	/**
	 * Stores a reference to the component's parent component
	 */
	private NGComponent _parent;

	/**
	 * Map of this component's children
	 *
	 * FIXME: Should we be initializing this here? // Hugi 2022-12-30
	 * FIXME: Could this initialization pose a potential race condition? // Hugi 2022-12-30
	 * FIXME: Should we use a string rather than the elementID class for the key? // Hugi 2022-12-30
	 */
	private Map<NGElementID, NGComponent> _children = new HashMap<>();

	/**
	 * Store a reference to the associations
	 */
	private Map<String, NGAssociation> _associations;

	/**
	 * Reference to the template contained within the element
	 */
	private NGElement _contentElement;

	/**
	 * Indicates that this is a page level element
	 *
	 * FIXME: I don't like having this here
	 */
	private boolean _isPage;

	public NGComponent( final NGContext context ) {
		Objects.requireNonNull( context );
		_context = context;
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
		_children.put( elementID, child );
	}

	/**
	 * @return The child with the given elementID. Null if none
	 *
	 * FIXME: Null? Really? // Hugi 2022-12-30
	 */
	public NGComponent getChild( NGElementID elementID ) {
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

	public NGContext context() {
		return _context;
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

	/**
	 * FIXME: I feel this should be private, since it's something only the framework should do (during the appendToResponse phase)
	 */
	public boolean isPage() {
		return _isPage;
	}

	/**
	 * FIXME: I feel this should be private, since it's something only the framework should do (during the appendToResponse phase)
	 */
	public void setIsPage( boolean value ) {
		_isPage = value;
	}

	@Override
	public NGResponse generateResponse() {
		final NGResponse response = new NGResponse();
		response.setHeader( "content-type", "text/html;charset=utf-8" ); // FIXME: This is most definitely not the place to set the encoding
		appendToResponse( response, context() );
		return response;
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		final NGComponent previouslyCurrentComponent = context.component();
		context.setCurrentComponent( this );
		template().takeValuesFromRequest( request, context );
		context.setCurrentComponent( previouslyCurrentComponent );
	}

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		final NGComponent previouslyCurrentComponent = context.component();
		context.setCurrentComponent( this );
		NGActionResults actionResults = template().invokeAction( request, context );
		context.setCurrentComponent( previouslyCurrentComponent );
		return actionResults;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		final NGComponent previouslyCurrentComponent = context.component();
		context.setCurrentComponent( this );
		template().appendToResponse( response, context );
		context.setCurrentComponent( previouslyCurrentComponent );
	}

	/**
	 * @return the template (which is stored by the component definition)
	 */
	public NGElement template() {
		return _componentDefinition.template();
	}
}