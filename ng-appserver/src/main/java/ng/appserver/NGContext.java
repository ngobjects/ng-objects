package ng.appserver;

import java.util.Objects;

public class NGContext {

	/**
	 * The request that initiated the creation of this context
	 */
	private final NGRequest _request;

	/**
	 * The response that will be constructed and/or  will be returned by this context.
	 *
	 * FIXME: Currently not used. Disabled until we decide what to do with it // Hugi 2023-01-11
	 */
	//	private NGResponse _response;

	/**
	 * The component currently being processed by this context
	 */
	private NGComponent _currentComponent;

	/**
	 * The page level component
	 */
	private NGComponent _page;

	/**
	 * This context's uniqueID within it's session
	 */
	private String _contextID;

	/**
	 * ID of the element currently being rendered by the context.
	 *
	 * FIXME: Rename to currentElementID?  // Hugi 2022-06-06
	 * FIXME: Not sure we want to initialize the elementID here. Cheaper to do elsewhere? // Hugi 2022-06-08
	 */
	private NGElementID _elementID = new NGElementID();

	/**
	 * The ID of the "originating context", i.e. the context that initiated the request we're currently handling
	 */
	private String _originatingContextID;

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
		Objects.requireNonNull( request );

		_request = request;
		request.setContext( this );

		// FIXME: This is not exactly a beautiful way to check if we're handling a component request // Hugi 2023-01-22
		if( request.uri().contains( "/wo/" ) ) {
			final String componentPart = request.parsedURI().getString( 1 );

			// The _originatingContextID is the first part of the request handler path
			_originatingContextID = componentPart.split( "\\." )[0];

			// The sending element ID consists of all the integers after the first one.
			_senderID = componentPart.substring( _originatingContextID.length() + 1 );
		}
	}

	public NGRequest request() {
		return _request;
	}

	/**
	 * FIXME: Currently not used. Disabled until we decide what to do with it // Hugi 2023-01-11
	 */
	//	public NGResponse response() {
	//		return _response;
	//	}

	/**
	 * @return This context's session, creating a session if none is present.
	 */
	@Deprecated
	public NGSession session() {
		return request().session();
	}

	/**
	 * @return This context's session, or null if no session is present.
	 *
	 * FIXME: This currently really only checks if session() has been invoked. We probably need to do a little deeper checking than this // Hugi 2023-01-07
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
	 * @return The page level component
	 */
	public NGComponent page() {
		return _page;
	}

	/**
	 * Set the page currently being rendered by this context.
	 *
	 *  FIXME: Can't we just assume that if we're setting the page, we're also setting the current component? OR should we always be explicit about that? // Hugi 2023-01-07
	 */
	public void setPage( NGComponent value ) {
		_page = value;
	}

	/**
	 * @return ID of the element currently being rendered by the context.
	 *
	 * FIXME: Take note of concurrency issues for lazy initialization // Hugi 2023-01-21
	 * FIXME: Why isn't the contextID an integer? Keeping it a string for now for WO code compaitibility // Hugi 2023-01-21
	 * FIXME: Should we check for an existing session before creating a contextID? Perhaps just always return zero for a session-less contextID? // Hugi 2023-01-21
	 */
	public String contextID() {
		if( _contextID == null ) {
			_contextID = String.valueOf( session().getContextIDAndIncrement() );
		}

		return _contextID;
	}

	/**
	 * @return The ID of the "original context", i.e. the context from which the request that created this context was initiated
	 *
	 * FIXME: This can probably be removed from here and just moved to NGComponentRequestHandler
	 */
	public String _originatingContextID() {
		return _originatingContextID;
	}

	/**
	 * @return ID of the element currently being rendered by the context.
	 */
	public NGElementID elementID() {
		return _elementID;
	}

	/**
	 * @return ID of the element being targeted by a component action
	 *
	 * FIXME: We should replace this with (or have an alternative) an NGElementID, to prevent constantly invoking NElementID.toString() in invokeAction() (when checking if the current element is the sender element) // Hugi 2023-02-09
	 */
	public String senderID() {
		return _senderID;
	}

	public boolean isInForm() {
		return _isInForm;
	}

	/**
	 * @return True if the current element is the sender
	 */
	public boolean currentElementIsSender() {
		return elementID().toString().equals( senderID() );
	}

	public void setIsInForm( boolean value ) {
		_isInForm = value;
	}

	/**
	 * @return The URL for invoking the action in the current context
	 *
	 * FIXME: This method is a symptom of component actions leaking into generic code, doesn't really belong // Hugi 2023-01-08
	 * FIXME: Make nice instead of ugly // Hugi 2023-01-08
	 */
	public String componentActionURL() {
		return "/wo/" + contextID() + "." + elementID();
	}

	@Override
	public String toString() {
		return "NGContext [_request=" + _request + ", _currentComponent=" + _currentComponent + ", _page=" + _page + ", _contextID=" + _contextID + ", _elementID=" + _elementID + ", _originatingContextID=" + _originatingContextID + ", _senderID=" + _senderID + ", _isInForm=" + _isInForm + "]";
	}
}