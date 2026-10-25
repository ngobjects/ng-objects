package ng.testapp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.directactions.NGDirectAction;
import ng.appserver.http.NGMultipartRequest;
import ng.appserver.http.NGMultipartRequest.Part;
import ng.appserver.http.NGRequest;
import ng.appserver.http.NGResponses;
import ng.appserver.http.NGResponse;
import ng.testapp.components.TAIncomingPartsPage;
import ng.testapp.components.TAIncomingPartsPage.ReceivedPart;
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

	/**
	 * Reads an uploaded form's parts as they arrive (see TAIncomingPartsPage). Each part is read to its end before the next is asked for; here we only count its bytes, but this is where you'd store it.
	 */
	public NGActionResults incomingPartsAction() {
		final TAIncomingPartsPage page = pageWithName( TAIncomingPartsPage.class );

		if( request() instanceof NGMultipartRequest multipartRequest ) {
			final List<ReceivedPart> receivedParts = new ArrayList<>();

			for( final Part part : multipartRequest.incomingParts() ) {
				final long started = System.currentTimeMillis();

				try {
					final long length = part.stream().transferTo( OutputStream.nullOutputStream() );
					receivedParts.add( new ReceivedPart( part.name(), part.filename(), part.contentType(), length, System.currentTimeMillis() - started ) );
				}
				catch( IOException e ) {
					throw new UncheckedIOException( e );
				}
			}

			page.receivedParts = receivedParts;

			// The query string's form values are available, even when the parts have been read as they arrived
			page.folder = multipartRequest.formValueForKey( "folder" );
		}

		return page;
	}

	/**
	 * Receives an upload whose body is the file itself (see TARawUploadPage), reading it as a stream, a buffer at a time. Here we only measure and fingerprint it, but this is where you'd store it.
	 */
	public NGActionResults rawUploadAction() {
		try( final DigestInputStream in = new DigestInputStream( request().contentStream(), MessageDigest.getInstance( "SHA-256" ) )) {
			final long length = in.transferTo( OutputStream.nullOutputStream() );
			return NGResponses.ok( "Received %s bytes of %s (the request said %s)\nSHA-256 %s".formatted( length, request().contentType(), request().contentLength(), HexFormat.of().formatHex( in.getMessageDigest().digest() ) ) );
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
		catch( NoSuchAlgorithmException e ) {
			throw new IllegalStateException( e );
		}
	}
}
