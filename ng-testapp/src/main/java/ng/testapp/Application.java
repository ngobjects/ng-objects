package ng.testapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class Application extends NGApplication {

	private static Logger logger = LoggerFactory.getLogger( Application.class );

	public static void main( String[] args ) {
		NGApplication.main( args, Application.class );
	}

	@Override
	public String adaptorClassName() {
		return "ng.adaptor.raw.NGAdaptorRaw";
	}

	@Override
	public NGResponse dispatchRequest( NGRequest request ) {
		logger.info( "Headers {} ", request.headers() );
		logger.info( "Content: " + request.contentString() );
		final NGResponse response = super.dispatchRequest( request );
		return response;
	}
}