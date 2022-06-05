package ng.appserver;

public class NGContext {

	private final NGRequest _request;
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
	 * ID of the element currently being rendered by the context.
	 */
	private String _elementID;

	/**
	 * In the case of component actions, this is the elementID of the element that invoked the action (clicked a link, submitted a form etc)
	 *
	 * FIXME: I kind of feel like it should be the responsibility of the component request handler to maintain this. Component actions are leaking into the framework here.
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

	public NGComponent component() {
		return _currentComponent;
	}

	public void setCurrentComponent( NGComponent component ) {
		_currentComponent = component;
	}

	/**
	 * ID of the element currently being rendered by the context.
	 */
	public String elementID() {
		return _elementID;
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