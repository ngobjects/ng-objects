package ng.appserver.http;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGContext;

/**
 * A request whose body isn't multipart: none at all, a form-encoded form, or any other content
 */

class NGStandardRequest implements NGRequest {

	/**
	 * The request's line, headers and origin, and what's parsed from them: everything about the request apart from its body
	 */
	private final NGRequestHead _head;

	/**
	 * The request's body
	 */
	private final NGRequestBody _body;

	private final NGRequestLimits _limits;

	/**
	 * The request's form values, from the query string and a form-encoded body. null until something asks for them.
	 */
	private Map<String, List<String>> _formValues;

	/**
	 * @param queryString The URI's query string, still percent-encoded. null if the URI has none.
	 * @param body The request's body. Readable only while the adaptor is handling the request.
	 */
	NGStandardRequest( final String method, final String uri, final String queryString, final Map<String, List<String>> headers, final InputStream body, final String remoteAddress, final NGRequestLimits limits ) {
		Objects.requireNonNull( limits );
		_head = new NGRequestHead( method, uri, queryString, headers, remoteAddress );
		_body = new NGRequestBody( body );
		_limits = limits;
	}

	@Override
	public String method() {
		return _head.method();
	}

	@Override
	public String uri() {
		return _head.uri();
	}

	@Override
	public void setURI( final String uri ) {
		_head.setURI( uri );
	}

	@Override
	public Map<String, List<String>> headers() {
		return _head.headers();
	}

	@Override
	public void setHeaders( final Map<String, List<String>> headers ) {
		_head.setHeaders( headers );
	}

	@Override
	public String remoteAddress() {
		return _head.remoteAddress();
	}

	@Override
	public Map<String, List<String>> cookieValues() {
		return _head.cookieValues();
	}

	@Override
	public NGContext context() {
		return _head.context( this );
	}

	@Override
	public void _setContext( final NGContext context ) {
		_head.setContext( context );
	}

	/**
	 * @return The request's form values: the query string's, followed by the body's if it's form-encoded (application/x-www-form-urlencoded). Only the query string's if the body was read as a stream. Parsed on first invocation.
	 * @throws IllegalArgumentException If the query string or the body has more fields, or the body more bytes, than the request's limits allow
	 */
	@Override
	public synchronized Map<String, List<String>> formValues() {
		if( _formValues == null ) {
			_formValues = parseFormValues();
		}

		return _formValues;
	}

	private Map<String, List<String>> parseFormValues() {
		final Map<String, List<String>> formValues = _head.parseQueryValues( _limits.maxFormFields() );
		final String contentType = contentType();

		if( contentType != null && "application/x-www-form-urlencoded".equalsIgnoreCase( NGRequestHead.mediaType( contentType ) ) && !_body.isStreamed() ) {
			final byte[] content = _body.bytes( (long)_limits.maxFormContentLength() );

			if( content.length > _limits.maxFormContentLength() ) {
				throw new IllegalArgumentException( "Form content is longer than %s bytes".formatted( _limits.maxFormContentLength() ) );
			}

			final Map<String, List<String>> bodyValues = NGFormValues.parse( content, NGRequestHead.charset( contentType ), _limits.maxFormFields() );

			for( final Entry<String, List<String>> entry : bodyValues.entrySet() ) {
				formValues.computeIfAbsent( entry.getKey(), _unused -> new ArrayList<>() ).addAll( entry.getValue() );
			}
		}

		return formValues;
	}

	@Override
	public byte[] contentBytes() {
		return _body.bytes( null ).clone();
	}

	@Override
	public InputStream contentStream() {
		return _body.stream( "contentStream()" );
	}

	@Override
	public String toString() {
		return "NGStandardRequest [" + _head.describe() + "]";
	}
}
