package ng.appserver.http;

/**
 * Utility methods for building response
 *
 *  FIXME: Temporary bridge class until response generation is standardized
 */

public class NGResponses {

	/**
	 * Creates an empty NGResponse with status 200
	 */
	@Deprecated
	public static NGResponse of() {
		return new NGStandardResponse();
	}

	/**
	 * @return A response with the given status and string content. Status comes first: it reads like the response's status line, and leaves the trailing parameter for the content (the one that varies in type)
	 */
	public static NGStandardResponse of( final int status, final String contentString ) {
		return new NGStandardResponse( contentString, status );
	}

	/**
	 * @return A response with the given status and binary content
	 */
	public static NGResponse of( final int status, final byte[] bytes ) {
		return new NGStandardResponse( bytes, status );
	}

	/**
	 * @return A response with status 200 and the given string content
	 */
	public static NGStandardResponse ok( final String contentString ) {
		return of( 200, contentString );
	}

	/**
	 * @return A response with status 200 and the given binary content
	 */
	public static NGResponse ok( final byte[] bytes ) {
		return of( 200, bytes );
	}

	/**
	 * @deprecated Parameter order changed, use of( status, bytes ) or ok( bytes )
	 */
	@Deprecated
	public static NGResponse of( final byte[] bytes, final int status ) {
		return of( status, bytes );
	}

	/**
	 * @deprecated Parameter order changed, use of( status, contentString ) or ok( contentString )
	 */
	@Deprecated
	public static NGStandardResponse of( final String contentString, final int status ) {
		return of( status, contentString );
	}
}
