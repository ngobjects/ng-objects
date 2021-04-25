package ng.appserver;

public class NGContext {

	private final NGRequest _request;
	private NGResponse _response;
	private NGSession _session;
	private NGComponent _currentComponent;

	public NGContext( final NGRequest request ) {
		_request = request;
	}

	public NGResponse response() {
		return _response;
	}

	public NGRequest request() {
		return _request;
	}

	public NGSession session() {
		return _session;
	}

	public NGComponent component() {
		return _currentComponent;
	}
}