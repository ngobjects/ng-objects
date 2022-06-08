package ng.appserver;

public class NGComponentRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		request.context()._originalContext.page().takeValuesFromRequest( request, request.context() );
		return request.context()._originalContext.page().invokeAction( request, request.context() ).generateResponse();
	}
}