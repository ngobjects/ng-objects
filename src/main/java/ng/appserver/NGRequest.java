package ng.appserver;

import java.util.List;
import java.util.Map;

/**
 * Represents a request entering the system 
 */

public class NGRequest extends NGMessage {

	/**
	 * FIXME: Shouldn't this really be an enum, or do we need to support arbitrary methods?
	 */
	public String _method;

	/**
	 * The URI being acessed
	 */
	public String _uri;
	
	/**
	 * FIXME: Should the value actually be a list of Strings? 
	 */
	public Map<String,List<String>> _headers;

	/**
	 * FIXME: Do we want to store this? Does parsing happen at the adaptor level or here?
	 */
	public Map<String,String> _formValues;
	
	/**
	 * The requests's content 
	 */
	public byte[] _content;

	public String uri() {
		return _uri;
	}
	
	public void setURI( final String uri ) {
		_uri = uri;
	}

	public NGRequest( final String method, final String uri, final Map<String,List<String>> headers, final byte[] content ) {
		_method = method;
		_uri = uri;
		_headers = headers;
		_content = content;
	}
}