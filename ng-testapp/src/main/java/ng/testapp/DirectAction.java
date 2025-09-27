package ng.testapp;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.directactions.NGDirectAction;
import ng.testapp.components.TAMain;
import ng.testapp.components.TAProgrammaticDynamicComponent;

public class DirectAction extends NGDirectAction {

	public DirectAction( NGRequest request ) {
		super( request );
	}

	@Override
	public NGActionResults defaultAction() {
		return new NGResponse( "Great success!", 200 );
	}

	public NGActionResults componentAction() {
		return pageWithName( TAMain.class );
	}

	public NGActionResults programmaticAction() {
		return pageWithName( TAProgrammaticDynamicComponent.class );
	}

	public NGActionResults htmlAction() {
		final NGResponse response = new NGResponse( "<html><head><meta charset=\"utf-8\"></head><body>Halló <strong>skralló!</strong></body></head>", 200 );
		response.setHeader( "content-type", "text/html" );
		response.setHeader( "yes", "sir" );
		response.setHeader( "yes", "doctor" );
		response.setHeader( "no", "ma'm" );
		return response;
	}

	public NGActionResults imageAction() {
		final byte[] imageBytes = NGApplication.application().resourceManager().obtainWebserverResource( "app", "test-image-1.jpg" ).get().bytes();
		final NGResponse response = new NGResponse( imageBytes, 200 );
		response.setHeader( "content-type", "image/jpeg" );
		return response;
	}
}