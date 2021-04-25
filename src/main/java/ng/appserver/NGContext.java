package ng.appserver;

public class NGContext {

	private final NGRequest _request;
	private NGResponse _response;
	private NGSession _session;

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
}