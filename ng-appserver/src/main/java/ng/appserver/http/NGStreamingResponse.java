package ng.appserver.http;

import java.io.InputStream;
import java.util.Objects;

public class NGStreamingResponse extends NGStandardResponse {

	/**
	 * Data to be streamed to the client
	 */
	private final InputStream _contentInputStream;

	/**
	 * Length of the stream to be streamed to the client
	 *
	 * The initial value is set to -1, meaning no content length has been set.
	 * We will check if the value has been set when returning the response, to ensure it's so.
	 */
	private final long _contentInputStreamLength;

	public NGStreamingResponse( final InputStream inputStream, final long length ) {
		Objects.requireNonNull( inputStream, "A streaming response can't be constructed with a null inputStream" );

		if( length < 0 ) {
			throw new IllegalArgumentException( "A streaming response's can't be constructed with negative length. You passed in " + length );
		}

		_contentInputStream = inputStream;
		_contentInputStreamLength = length;
	}

	public InputStream contentInputStream() {
		return _contentInputStream;
	}

	public long contentInputStreamLength() {
		return _contentInputStreamLength;
	}
}