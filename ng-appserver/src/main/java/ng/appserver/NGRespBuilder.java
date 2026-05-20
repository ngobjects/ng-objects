package ng.appserver;

/**
 * Utility methods for building response
 *
 *  FIXME: Temporary bridge class until response generation is standardized
 */

@Deprecated
public class NGRespBuilder {

	/**
	 * Creates an empty NGResponse with status 200
	 */
	@Deprecated
	public static NGResponse of() {
		return new NGStandardResponse();
	}

	@Deprecated
	public static NGResponse of( final byte[] bytes, final int status ) {
		return new NGStandardResponse( bytes, status );
	}

	@Deprecated
	public static NGStandardResponse of( final String contentString, final int status ) {
		return new NGStandardResponse( contentString, status );
	}
}