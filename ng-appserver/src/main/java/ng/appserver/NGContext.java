package ng.appserver;

import java.util.Objects;

public class NGContext {

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
	private NGComponent _currentComponent;

	/**
	 * ID of the element currently being rendered by the context.
	 */
	private NGElementID _currentElementID;

	/**
	 * The ID of the "originating context", i.e. the context that initiated the request we're currently handling
	 */
	private String _originatingContextID;

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

		_currentElementID = new NGElementID();

		// FIXME: This is not exactly a beautiful way to check if we're handling a component request
		// This code probably belongs in the NGComponentRequestHandler
		// Hugi 2023-01-22
		if( request.uri().startsWith( NGComponentRequestHandler.DEFAULT_PATH ) ) {
			// Component action URLs contain only one path element, which contains both the originating contextID and the senderID.
			final String componentPart = request.parsedURI().getString( 1 );

			// The contextID and the elementID are separated by a period, so let's split on that.
			final int firstPeriodIndex = componentPart.indexOf( '.' );

			// The _originatingContextID is the first part of the request handler path. This tells us where the request is coming from.
			_originatingContextID = componentPart.substring( 0, firstPeriodIndex );

			// The sending element ID consists of everything after the first period.
			_senderID = NGElementID.fromString( componentPart.substring( firstPeriodIndex + 1 ) );
		}
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
		return _currentComponent;
	}

	/**
	 * Set the component currently being rendered in this context
	 */
	public void setComponent( final NGComponent component ) {
		_currentComponent = component;
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
	 * FIXME: Take note of concurrency issues for lazy initialization // Hugi 2023-01-21
	 */
	public String contextID() {
		if( _contextID == null ) {
			_contextID = String.valueOf( session().getContextIDAndIncrement() );
		}

		return _contextID;
	}

	/**
	 * @return The ID of the "original context", i.e. the context from which the request that created this context was initiated
	 */
	public String _originatingContextID() {
		return _originatingContextID;
	}

	/**
	 * @return ID of the element currently being rendered by the context.
	 */
	public NGElementID elementID() {
		return _currentElementID;
	}

	/**
	 * @return ID of the element being targeted by a component action
	 */
	public NGElementID senderID() {
		return _senderID;
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
		return "NGContext [_request=" + _request + ", _currentComponent=" + _currentComponent + ", _page=" + _page + ", _contextID=" + _contextID + ", _elementID=" + _currentElementID + ", _originatingContextID=" + _originatingContextID + ", _senderID=" + _senderID + ", _isInForm=" + _isInForm + "]";
	}
}