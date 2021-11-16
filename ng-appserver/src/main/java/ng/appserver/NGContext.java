package ng.appserver;

public class NGContext {

	private final NGRequest _request;
	private NGResponse _response;
	private NGSession _session;

	/**
	 * The component currently being processed by this context
	 */
	private NGComponent _currentComponent;

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
		return _session;
	}

	public NGComponent component() {
		return _currentComponent;
	}

	public void setCurrentComponent( NGComponent component ) {
		_currentComponent = component;
	}
}