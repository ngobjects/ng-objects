package ng.appserver;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import ng.appserver.templating.NGComponent;
import ng.appserver.templating.NGElementID;

public class NGContext {

	/**
	 * Contains the list of AjaxUpdateContainer IDs that encapsulate the element currently being rendered
	 *
	 * FIXME: This is a temporary hack while we're developing the AJAX functionality // Hugi 2024-03-16
	 */
	public Set<String> containingUpdateContainerIDs = new HashSet<>();

	/**
	 * FIXME: Yet another temporary testhack while we experiment with methods to control partial page updates on the server side // Hugi 2024-10-09
	 */
	public boolean forceFullUpdate;

	/**
	 * The request that initiated the creation of this context
	 */
	private final NGRequest _request;

	/**
	 * This context's uniqueID within it's session
	 */
	private String _contextID;

	/**
	 * Stores the ID of the context that originated the creation of this context.
	 * Will currently be null if it's the first context in a series of transactions.
	 * Serves the stateful page caching mechanism to track origin of partial pages.
	 *
	 * CHECKME: Still under consideration. Getting this out of the context itself might be nice // Hugi 2024-09-28
	 */
	private String _originatingContextID;

	/**
	 * The page level component
	 */
	private NGComponent _page;

	/**
	 * The component currently being processed by this context
	 */
	private NGComponent _component;

	/**
	 * ID of the element currently being rendered by the context.
	 */
	private NGElementID _elementID;

	/**
	 * In the case of component actions, this is the elementID of the element that invoked the action (clicked a link, submitted a form etc)
	 * Used in combination with _requestContextID to find the proper action to initiate.
	 */
	private NGElementID _senderID;

	/**
	 * Indicates that this context should be saved in the page cache.
	 * Currently, this just means that a context ID has been generated by invoking contextID().
	 * That should usually only happen during the generation of a stateful action URL.
	 */
	private boolean _shouldSaveInPageCache;

	public NGContext( final NGRequest request ) {
		Objects.requireNonNull( request );
		_request = request;
		request.setContext( this );

		// CHECKME: We only need an elementID if we're going to be rendering a component, so theoretically, this could be initialized lazily
		_elementID = new NGElementID();
	}

	/**
	 * @return The request that this context originates from
	 */
	public NGRequest request() {
		return _request;
	}

	/**
	 * @return This context's session, creating a session if none is present.
	 */
	@Deprecated
	public NGSession session() {
		return request().session();
	}

	/**
	 * @return This context's session, or null if no session is present.
	 */
	@Deprecated
	public NGSession existingSession() {
		return request().existingSession();
	}

	/**
	 * @return True if this context has an existing session
	 */
	@Deprecated
	public boolean hasSession() {
		return request().hasSession();
	}

	/**
	 * @return The component currently being rendered in this context
	 */
	public NGComponent component() {
		return _component;
	}

	/**
	 * Set the component currently being rendered in this context
	 */
	public void setComponent( final NGComponent component ) {
		_component = component;
	}

	/**
	 * @return The page level component
	 */
	public NGComponent page() {
		return _page;
	}

	/**
	 * Set the page currently being rendered by this context.
	 */
	public void setPage( NGComponent value ) {
		_page = value;
	}

	/**
	 * @return ID of the element currently being rendered by the context.
	 *
	 * CHECKME: Take note of concurrency issues for lazy initialization // Hugi 2023-01-21
	 */
	public String contextID() {
		if( _contextID == null ) {
			_contextID = String.valueOf( session().getContextIDAndIncrement() );

			// Since we're generating a contextID we're going to have to assume our page instance has to be cached, so we mark it as such.
			//
			// CHECKME: This is currently just the best indiator we have that we're going to be involved in stateful work.
			// (i.e. the page was a result of a stateful action invocation, or has generated stateful URLs that reference it)
			// Feels like we could be a little more explicit about this, since at the moment it feels a little too much like a side effect.
			_shouldSaveInPageCache = true;
		}

		return _contextID;
	}

	/**
	 * Resets the current elementID
	 */
	public void _resetElementID() {
		_elementID = new NGElementID();
	}

	/**
	 * @return ID of the element currently being rendered by the context.
	 */
	public NGElementID elementID() {
		return _elementID;
	}

	/**
	 * @return ID of the element being targeted by a component action
	 */
	public NGElementID senderID() {
		return _senderID;
	}

	/**
	 * Set the senderID
	 */
	public void _setSenderIDFromString( final String senderIDString ) {
		_senderID = NGElementID.fromString( senderIDString );
	}

	/**
	 * @return True if the current element is the sender
	 */
	public boolean currentElementIsSender() {
		return senderID() != null && senderID().equals( elementID() );
	}

	/**
	 * Set the ID of the context that originated the creation of this context.
	 */
	public String _originatingContextID() {
		return _originatingContextID;
	}

	/**
	 * Set the ID of the context that originated the creation of this context.
	 */
	public void _setOriginatingContextID( String value ) {
		_originatingContextID = value;
	}

	/**
	 * @return true if a contextID has been generated for this context, meaning we might want to access that context again.
	 */
	public boolean _shouldSaveInPageCache() {
		return _shouldSaveInPageCache;
	}

	/**
	 * @return The URL for invoking the action in the current context
	 */
	public String componentActionURL() {
		return NGComponentRequestHandler.DEFAULT_PATH + contextID() + "." + elementID();
	}

	/**
	 * ID of the update container targeted with this request
	 *
	 * FIXME:
	 * Should be replaced with a variable, preferably set at the context's construction.
	 * That also means we need to make the context's construction a little more formal, since any request handler should be able to request partial rendering (and it should be straight forward).
	 * In any case, this functionality will probably end up in a separate rendering context that's been begging to be created for a while.
	 * // Hugi 2024-10-15
	 */
	public String targetedUpdateContainerID() {
		return request().headerForKey( "x-updatecontainerid" );
	}

	@Override
	public String toString() {
		return "NGContext [_request=" + _request + ", _component=" + _component + ", _page=" + _page + ", _contextID=" + _contextID + ", _elementID=" + _elementID + ", _senderID=" + _senderID + "]";
	}
}