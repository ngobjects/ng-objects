package ng.appserver.http;

import java.io.InputStream;

/**
 * Utility methods for building response
 *
 *  FIXME: Temporary bridge class until response generation is standardized
 */

public class NGResponses {

	/**
	 * Creates an empty NGResponse with status 200
	 */
	public static NGResponse of() {
		return new NGStandardResponse();
	}

	/**
	 * @return A response with the given status and string content. Status comes first: it reads like the response's status line, and leaves the trailing parameter for the content (the one that varies in type)
	 */
	public static NGResponse of( final int status, final String contentString ) {
		final NGStandardResponse response = new NGStandardResponse();
		response.setStatus( status );
		response.setContentString( contentString );
		return response;
	}

	/**
	 * @return A response with the given status and binary content
	 */
	public static NGResponse of( final int status, final byte[] bytes ) {
		final NGStandardResponse response = new NGStandardResponse();
		response.setStatus( status );
		response.setContentBytes( bytes );
		return response;
	}

	/**
	 * @return A response with status 200 and the given string content
	 */
	public static NGResponse ok( final String contentString ) {
		return of( 200, contentString );
	}

	/**
	 * @return A response with status 200 and the given binary content
	 */
	public static NGResponse ok( final byte[] bytes ) {
		return of( 200, bytes );
	}

	public static NGResponse streaming( final InputStream inputStream, final long length ) {
		return new NGStreamingResponse( inputStream, length );
	}
}