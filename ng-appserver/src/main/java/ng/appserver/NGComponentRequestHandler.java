package ng.appserver;

public class NGComponentRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		final NGContext originalContext = request.context().session().contexts.get( Integer.parseInt( request.context().requestContextID() ) );
		System.out.println( originalContext.page().getClass().getName() );
		originalContext.page().takeValuesFromRequest( request, request.context() );
		return originalContext.page().invokeAction( request, request.context() ).generateResponse();
	}
}