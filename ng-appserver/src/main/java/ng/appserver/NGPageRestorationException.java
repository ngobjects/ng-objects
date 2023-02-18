package ng.appserver;

public class NGPageRestorationException extends RuntimeException {

	private final NGRequest _request;

	public NGPageRestorationException( NGRequest request, String message ) {
		super( message );
		_request = request;
	}

	public NGRequest request() {
		return _request;
	}
}