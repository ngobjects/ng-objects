package ng.appserver.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import ng.appserver.NGContext;

/**
 * A request whose body is bytes: none at all, a form-encoded form, or any other content
 */

public class NGStandardRequest implements NGRequest {

	/**
	 * The most fields read from the query string, and from a form-encoded body
	 */
	public static final int MAX_FORM_FIELDS = 1000;

	/**
	 * The largest form-encoded body read for form values, in bytes
	 */
	public static final int MAX_FORM_CONTENT_LENGTH = 200_000;

	/**
	 * The request's line, headers and origin, and what's parsed from them: everything about the request apart from its body
	 */
	private final NGRequestHead _head;

	/**
	 * The request's form values, from the query string and a form-encoded body. null until something asks for them.
	 */
	private Map<String, List<String>> _formValues;

	/**
	 * The request's body as received from the adaptor, read on first access to the content. Readable only while the adaptor is handling the request.
	 */
	private final InputStream _contentStream;

	/**
	 * The request's body, once read. null until something asks for the content.
	 */
	private ByteArrayOutputStream _contentByteStream;

	/**
	 * Creates a request whose form values are parsed from the query string and, when the body is form-encoded, the body, the first time they're asked for.
	 *
	 * @param queryString The URI's query string, still percent-encoded. null if the URI has none.
	 * @param contentStream The request's body. Readable only while the adaptor is handling the request.
	 */
	public NGStandardRequest( final String method, final String uri, final String queryString, final String httpVersion, final Map<String, List<String>> headers, final InputStream contentStream, String remoteAddress ) {
		Objects.requireNonNull( contentStream );
		_head = new NGRequestHead( method, uri, queryString, httpVersion, headers, remoteAddress );
		_contentStream = contentStream;
	}

	@Override
	public String method() {
		return _head.method();
	}

	/**
	 * @return the request's URL
	 */
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
	 * @return The request's form values: the query string's, followed by the body's if it's form-encoded (application/x-www-form-urlencoded). Parsed on first invocation.
	 * @throws IllegalArgumentException If the query string or the body has more than MAX_FORM_FIELDS fields, or the body is longer than MAX_FORM_CONTENT_LENGTH
	 */
	@Override
	public synchronized Map<String, List<String>> formValues() {
		if( _formValues == null ) {
			_formValues = parseFormValues();
		}

		return _formValues;
	}

	/**
	 * @return The request's form values, parsed. Invoked once, by formValues().
	 */
	protected Map<String, List<String>> parseFormValues() {
		final Map<String, List<String>> formValues = _head.parseQueryValues();
		final String contentType = _head.contentType();

		if( contentType != null && "application/x-www-form-urlencoded".equalsIgnoreCase( NGRequestHead.mediaType( contentType ) ) ) {
			final Map<String, List<String>> bodyValues = NGFormValues.parse( readContent( MAX_FORM_CONTENT_LENGTH ).toByteArray(), NGRequestHead.charset( contentType ), MAX_FORM_FIELDS );

			for( final Entry<String, List<String>> entry : bodyValues.entrySet() ) {
				formValues.computeIfAbsent( entry.getKey(), _unused -> new ArrayList<>() ).addAll( entry.getValue() );
			}
		}

		return formValues;
	}

	/**
	 * @return The request's body, read from the adaptor's stream on first invocation
	 */
	@Override
	public synchronized byte[] contentBytes() {
		return readContent( Long.MAX_VALUE ).toByteArray();
	}

	/**
	 * @return The request's content, read from the adaptor's stream if it hasn't been yet
	 * @throws IllegalArgumentException If the content has to be read and turns out longer than maxLength bytes
	 */
	private synchronized ByteArrayOutputStream readContent( final long maxLength ) {
		if( _contentByteStream == null ) {
			final ByteArrayOutputStream contentByteStream = new ByteArrayOutputStream();

			try( _contentStream) {
				final byte[] buffer = new byte[8192];
				long length = 0;
				int read;

				while( (read = _contentStream.read( buffer )) != -1 ) {
					length += read;

					if( length > maxLength ) {
						throw new IllegalArgumentException( "Content is longer than %s bytes".formatted( maxLength ) );
					}

					contentByteStream.write( buffer, 0, read );
				}
			}
			catch( IOException e ) {
				throw new UncheckedIOException( e );
			}

			_contentByteStream = contentByteStream;
		}

		return _contentByteStream;
	}

	@Override
	public String toString() {
		return "NGStandardRequest [" + _head.describe() + "]";
	}
}
