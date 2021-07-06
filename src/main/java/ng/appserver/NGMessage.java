package ng.appserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parent class of NGResponse/NGRequest 
 */

public abstract class NGMessage {

	/**
	 * Stores the HTTP version of this request/respons
	 */
	private String _httpVersion;

	/**
	 * FIXME: Probably don't want to populate this with an empty map at the start?
	 * FIXME: Thread safety!
	 */
	private Map<String,List<String>> _headers = new HashMap<>();


	public String httpVersion() {
		return _httpVersion;
	}
	
	public void setHttpVersion( final String value ) {
		_httpVersion = value;
	}
	
	public Map<String,List<String>> headers() {
		return _headers;
	}

	public void setHeaders( Map<String,List<String>> headers ) {
		_headers = headers;
	}
	
	public void setHeader( final String key, final String value ) {
		List<String> list = headers().get( key );
		
		// FIXME: This implicitly mutable thing is... not good
		if( list == null ) {
			list = new ArrayList<>();
			headers().put( key, list );
		}
		
		list.add( value );
	}
}