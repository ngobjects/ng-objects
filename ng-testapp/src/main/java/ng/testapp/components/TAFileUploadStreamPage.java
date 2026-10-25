package ng.testapp.components;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

/**
 * Receiving an uploaded file as a stream, through NGFileUpload's [inputStream] binding, so the file is never held in memory as a whole.
 *
 * The stream is readable until the request has been handled (the framework keeps a large file in a temporary file until then), so read it in the action, or copy it somewhere that outlives the request.
 */

public class TAFileUploadStreamPage extends NGComponent {

	public record ReceivedFile( String filename, long length, String sha256 ) {}

	public List<ReceivedFile> receivedFiles = new ArrayList<>();
	public ReceivedFile currentFile;

	/**
	 * Set by the upload field when the form is submitted, before the receive() action is invoked
	 */
	public InputStream uploadedStream;
	public String uploadedFilename;

	public TAFileUploadStreamPage( NGContext context ) {
		super( context );
	}

	/**
	 * Reads the uploaded file through a digest, a buffer at a time. Here we only measure and fingerprint it, but this is where you'd store it.
	 */
	public NGActionResults receive() {
		if( uploadedStream == null ) {
			return null;
		}

		try( final DigestInputStream stream = new DigestInputStream( uploadedStream, MessageDigest.getInstance( "SHA-256" ) )) {
			final long length = stream.transferTo( OutputStream.nullOutputStream() );
			receivedFiles.add( new ReceivedFile( uploadedFilename, length, HexFormat.of().formatHex( stream.getMessageDigest().digest() ) ) );
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
		catch( NoSuchAlgorithmException e ) {
			throw new IllegalStateException( e );
		}

		uploadedStream = null;
		uploadedFilename = null;

		return null;
	}
}
