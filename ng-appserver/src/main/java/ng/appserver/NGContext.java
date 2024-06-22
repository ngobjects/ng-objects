package ng.appserver;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class NGContext {

	/**
	 * Contains the list of AjaxUpdateContainer IDs that encapsulate the element currently being rendered
	 *
	 * FIXME: This is a temporary hack while we're developing the AJAX functionality // Hugi 2024-03-16
	 */
	public Set<String> updateContainerIDs = new HashSet<>();

	/**
	 * The request that initiated the creation of this context
	 */
	private final NGRequest _request;

	/**
	 * This context's uniqueID within it's session
	 */
	private String _contextID;

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
	 * Indicates the the context is currently rendering something nested inside a form element.
	 */
	private boolean _isInForm;

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
		return elementID().equals( senderID() );
	}

	/**
	 * @return true if we're currently rendering inside a form
	 */
	public boolean isInForm() {
		return _isInForm;
	}

	/**
	 * Set by NGForm to indicate if we're inside a form or not.
	 */
	public void setIsInForm( boolean value ) {
		_isInForm = value;
	}

	/**
	 * @return The URL for invoking the action in the current context
	 */
	public String componentActionURL() {
		return NGComponentRequestHandler.DEFAULT_PATH + contextID() + "." + elementID();
	}

	@Override
	public String toString() {
		return "NGContext [_request=" + _request + ", _component=" + _component + ", _page=" + _page + ", _contextID=" + _contextID + ", _elementID=" + _elementID + ", _senderID=" + _senderID + ", _isInForm=" + _isInForm + "]";
	}
}