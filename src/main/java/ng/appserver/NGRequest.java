package ng.appserver;

public class NGRequest extends NGMessage {

	public String _uri;
	
	public String uri() {
		return _uri;
	}
	
	public void setURI( final String uri ) {
		_uri = uri;
	}
}