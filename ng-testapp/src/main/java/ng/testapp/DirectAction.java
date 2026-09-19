package ng.testapp;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.directactions.NGDirectAction;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGResponses;
import ng.appserver.http.NGResponse;
import ng.testapp.components.TAMain;
import ng.testapp.components.TAProgrammaticDynamicComponent;

public class DirectAction extends NGDirectAction {

	public DirectAction( NGRequest request ) {
		super( request );
	}

	@Override
	public NGActionResults defaultAction() {
		return NGResponses.ok( "Great success!" );
	}

	public NGActionResults componentAction() {
		return pageWithName( TAMain.class );
	}

	public NGActionResults programmaticAction() {
		return pageWithName( TAProgrammaticDynamicComponent.class );
	}

	public NGActionResults htmlAction() {
		final NGResponse response = NGResponses.ok( "<html><head><meta charset=\"utf-8\"></head><body>Halló <strong>skralló!</strong></body></head>" );
		response.setHeader( "content-type", "text/html" );
		response.setHeader( "yes", "sir" );
		response.setHeader( "yes", "doctor" );
		response.setHeader( "no", "ma'm" );
		return response;
	}

	public NGActionResults imageAction() {
		final byte[] imageBytes = NGApplication.application().resourceManager().obtainWebserverResource( "app", "test-image-1.jpg" ).get().bytes();
		final NGResponse response = NGResponses.ok( imageBytes );
		response.setHeader( "content-type", "image/jpeg" );
		return response;
	}
}