package ng.testapp;

import ng.appserver.NGApplication;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class Application extends NGApplication {

	public static void main( String[] args ) {
		NGApplication.main( args, Application.class );
	}

	@Override
	public String adaptorClassName() {
		return "ng.adaptor.raw.NGAdaptorRaw";
	}

	@Override
	public NGResponse dispatchRequest( NGRequest request ) {
		System.out.println( request.headers() );
		final NGResponse response = super.dispatchRequest( request );
		return response;
	}
}