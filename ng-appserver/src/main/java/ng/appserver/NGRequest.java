package ng.appserver;

import java.util.Collections;
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
	private final Map<String, List<String>> _formValues;

	/**
	 * The requests's content
	 *
	 * FIXME: Should this be a byte array? I'm thinking no.
	 */
	public byte[] _content;

	public Map<String, List<String>> formValues() {
		return _formValues;
	}

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

	public NGRequest( final String method, final String uri, final String httpVersion, final Map<String, List<String>> headers, final byte[] content ) {
		setMethod( method );
		setURI( uri );
		setHttpVersion( httpVersion );
		setHeaders( headers );
		_content = content;

		// FIXME: We're going to have to do some work here (or rather, not here, but lazily done in formValues()) to parse the form values based on the request's type/encoding etc.
		_formValues = Collections.emptyMap();
	}
}