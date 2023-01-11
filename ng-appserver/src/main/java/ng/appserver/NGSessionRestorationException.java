package ng.appserver;

public class NGSessionRestorationException extends RuntimeException {

	private final NGRequest _request;

	public NGSessionRestorationException( NGRequest request ) {
		_request = request;
	}

	public NGRequest request() {
		return _request;
	}
}