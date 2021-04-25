package ng.appserver;

public class NGResourceRequestHandler extends NGRequestHandler {

	@Override
	public NGResponse handleRequest( final NGRequest request ) {
		byte[] bytes = NGApplication.application().resourceManager().bytesForResourceWithName( "test.jpg" );

		return new NGResponse( bytes, 200 );
	}
}