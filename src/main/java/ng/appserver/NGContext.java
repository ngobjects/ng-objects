package ng.appserver;

public class NGContext {

	private final NGRequest _request;
	private NGResponse _response;

	/**
	 * FIXME: This should probably not be public 
	 */
	public NGContext( final NGRequest request ) {
		_request = request;
	}
	
	public NGResponse response() {
		return _response;
	}
	
	public NGRequest request() {
		return _request;
	}
}