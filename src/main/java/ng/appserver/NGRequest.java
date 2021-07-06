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
	private String _method;

	/**
	 * The URI being acessed
	 */
	private String _uri;

	/**
	 * FIXME: Do we want to store this? Does parsing happen at the adaptor level or here?
	 */
	private Map<String,String> _formValues;

	/**
	 * The requests's content 
	 * 
	 * FIXME: Should this be a byte array? I'm thinking no.
	 */
	public byte[] _content;

	public String uri() {
		return _uri;
	}
	
	public void setURI( final String uri ) {
		_uri = uri;
	}

	public String method() {
		return _method;
	}

	public void setMethod( final String method ) {
		_method = method;
	}

	public NGRequest( final String method, final String uri, final Map<String,List<String>> headers, final byte[] content ) {
		setMethod( method );
		setURI( uri );
		setHeaders( headers );
		_content = content;
	}
}