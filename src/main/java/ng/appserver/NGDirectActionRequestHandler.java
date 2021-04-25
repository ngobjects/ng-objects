package ng.appserver;

public class NGDirectActionRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		return new NGResponse("Sweet!");
	}
}