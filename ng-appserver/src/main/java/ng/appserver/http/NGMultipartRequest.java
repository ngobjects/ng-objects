package ng.appserver.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A multipart/form-data request. Its content is a list of parts: a form's fields, and the files uploaded with it.
 *
 * The parts are read, by the adaptor, the first time something asks for the request's form values or uploaded files.
 */

public class NGMultipartRequest extends NGStandardRequest {

	/**
	 * One part of the request's content, as read by the adaptor
	 *
	 * @param name The name of the form field the part belongs to
	 * @param filename The uploaded file's name, if the part is a file (even an empty one). null if the part is a plain field.
	 * @param contentType The part's content type. null if it has none.
	 * @param stream The part's content
	 * @param length The part's length in bytes
	 */
	public record Part( String name, String filename, String contentType, InputStream stream, long length ) {}

	/**
	 * Reads the request's parts. Invoked once, the first time they're needed.
	 */
	private final Supplier<List<Part>> _partsReader;

	/**
	 * The request's parts, once read. null until then.
	 */
	private List<Part> _parts;

	/**
	 * The request's uploaded files by name of field, once read. null until then.
	 */
	private Map<String, List<NGUploadedFile>> _uploadedFiles;

	/**
	 * @param partsReader Reads the request's parts. Invoked at most once, and only while the adaptor is handling the request.
	 */
	public NGMultipartRequest( final String method, final String uri, final String queryString, final String httpVersion, final Map<String, List<String>> headers, final Map<String, List<String>> cookieValues, final Supplier<List<Part>> partsReader, final String remoteAddress ) {
		super( method, uri, queryString, httpVersion, headers, cookieValues, InputStream.nullInputStream(), remoteAddress );
		Objects.requireNonNull( partsReader );
		_partsReader = partsReader;
	}

	private synchronized List<Part> parts() {
		if( _parts == null ) {
			_parts = _partsReader.get();
		}

		return _parts;
	}

	/**
	 * @return The query string's form values, followed by the values of the form's fields (the parts that aren't files)
	 */
	@Override
	protected Map<String, List<String>> parseFormValues() {
		final Map<String, List<String>> formValues = super.parseFormValues();

		for( final Part part : parts() ) {
			if( part.filename() == null ) {
				formValues.computeIfAbsent( part.name(), _unused -> new ArrayList<>() ).add( contentString( part ) );
			}
		}

		return formValues;
	}

	/**
	 * @return The given part's content, as a string in the charset named by its content type (UTF-8 if it names none)
	 */
	private static String contentString( final Part part ) {
		try( final InputStream stream = part.stream()) {
			return new String( stream.readAllBytes(), part.contentType() == null ? StandardCharsets.UTF_8 : charset( part.contentType() ) );
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * @return The uploaded files, by the name of the form field they were uploaded with
	 */
	public synchronized Map<String, List<NGUploadedFile>> uploadedFiles() {
		if( _uploadedFiles == null ) {
			final Map<String, List<NGUploadedFile>> uploadedFiles = new LinkedHashMap<>();

			for( final Part part : parts() ) {
				if( part.filename() != null ) {
					uploadedFiles.computeIfAbsent( part.name(), _unused -> new ArrayList<>() ).add( new NGUploadedFile( part.filename(), part.contentType(), part.stream(), part.length() ) );
				}
			}

			_uploadedFiles = uploadedFiles;
		}

		return _uploadedFiles;
	}

	/**
	 * @return The files uploaded with the named field. An empty list if there are none.
	 */
	public List<NGUploadedFile> uploadedFilesForKey( final String key ) {
		final List<NGUploadedFile> files = uploadedFiles().get( key );

		if( files == null ) {
			return Collections.emptyList();
		}

		return files;
	}

	/**
	 * @return The file uploaded with the named field. null if there's none.
	 * @throws IllegalStateException If more than one file was uploaded with the field
	 */
	public NGUploadedFile uploadedFileForKey( final String key ) {
		final List<NGUploadedFile> files = uploadedFilesForKey( key );

		if( files.isEmpty() ) {
			return null;
		}

		if( files.size() > 1 ) {
			throw new IllegalStateException( "The request contains %s files uploaded with the field '%s'. If you expected multiple files, use uploadedFilesForKey() instead of uploadedFileForKey()".formatted( files.size(), key ) );
		}

		return files.get( 0 );
	}

	/**
	 * A multipart request's content is its parts, read through formValues() and uploadedFiles()
	 *
	 * @throws UnsupportedOperationException Always
	 */
	@Override
	public ByteArrayOutputStream contentByteStream() {
		throw new UnsupportedOperationException( "A multipart request's content is its parts. Use formValues() and uploadedFiles()" );
	}
}
