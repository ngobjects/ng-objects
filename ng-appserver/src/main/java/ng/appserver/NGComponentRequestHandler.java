package ng.appserver;

public class NGComponentRequestHandler extends NGRequestHandler {

	/**
	 * Just a little experiment, instead of a page cache
	 */
	public static NGComponent currentPage;

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		NGResponse response = new NGResponse();
		currentPage.appendToResponse( response, request.context() );
		return response;
	}
}