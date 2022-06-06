package ng.appserver;

public class NGContext {

	/**
	 * The request that initiated the creation of this context
	 */
	private final NGRequest _request;

	/**
	 * The response that will be constructed and/or  will be returned by this context.
	 */
	private NGResponse _response;

	/**
	 * Stores the context's session
	 */
	private NGSession _session;

	/**
	 * The component currently being processed by this context
	 */
	private NGComponent _currentComponent;

	/**
	 * This context's uniqueID within it's session
	 */
	private String _contextID;

	/**
	 * ID of the element currently being rendered by the context.
	 *
	 * FIXME: Rename to currentElementID?
	 */
	private String _elementID;

	/**
	 * The ID of the context that initiated the request In the case of component actions, this will be used to restore the page we're working in.
	 *
	 * FIXME: Should be in the request instead? Or somehow handled by not storing this within the context?
	 */
	private String _requestContextID;

	/**
	 * In the case of component actions, this is the elementID of the element that invoked the action (clicked a link, submitted a form etc)
	 * Used in combination with _requestContextID to find the proper action to initiate.
	 *
	 * FIXME: I kind of feel like it should be the responsibility of the component request handler to maintain this. Component actions are leaking into the framework here.
	 * FIXME: Rename to _requestElementID to mirror the naming of _requestContextID?
	 */
	private String _senderID;

	/**
	 * Indicates the the context is currently rendering something nested inside a form element.
	 */
	private boolean _isInForm;

	public NGContext( final NGRequest request ) {
		_request = request;
	}

	public NGRequest request() {
		return _request;
	}

	public NGResponse response() {
		return _response;
	}

	public NGSession session() {
		if( _session == null && _request._extractSessionID() != null ) {
			_session = NGApplication.application().sessionStore().checkoutSessionWithID( _request._extractSessionID() );
		}

		return _session;
	}

	/**
	 * @return The component currently being rendered by this context
	 *
	 * FIXME: Initially called component(). to reflect the WO naming. Perhaps better called currentComponent() to reflect it's use better?
	 */
	public NGComponent component() {
		return _currentComponent;
	}

	public void setCurrentComponent( NGComponent component ) {
		_currentComponent = component;
	}

	/**
	 * ID of the element currently being rendered by the context.
	 */
	public String contextID() {
		return _contextID;
	}

	/**
	 * ID of the element currently being rendered by the context.
	 */
	public String elementID() {
		return _elementID;
	}

	public void setElementID( String value ) {
		_elementID = value;
	}

	public String requestContextID() {
		return _requestContextID;
	}

	public void setRequestContextID( String value ) {
		_requestContextID = value;
	}

	public String senderID() {
		return _senderID;
	}

	public void setSenderID( String value ) {
		_senderID = value;
	}

	public boolean isInForm() {
		return _isInForm;
	}

	public void setIsInForm( boolean value ) {
		_isInForm = value;
	}
}