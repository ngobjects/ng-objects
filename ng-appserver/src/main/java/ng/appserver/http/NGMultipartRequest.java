package ng.appserver.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A multipart/form-data request. Its content is a list of parts: a form's fields, and the files uploaded with it.
 *
 * The parts can be read in one of two ways, whichever is asked for first:
 *
 * - Collected: formValues() and uploadedFiles() read every part before returning, keeping files' content (in memory or temporary files) until the request has been handled.
 * - Incoming: incomingParts() hands over the parts as they arrive, for reading in one pass. For files too large to collect, or to handle while they're still arriving.
 *   The form's fields then arrive as parts too, and formValues() holds only the query string's.
 */

public class NGMultipartRequest extends NGStandardRequest {

	/**
	 * One part of a multipart request
	 *
	 * @param name The name of the form field the part belongs to
	 * @param filename The uploaded file's name, if the part is a file. Empty when a form's file field was submitted without a file. null if the part is a plain field.
	 * @param headers The part's headers. Keys are case-insensitive.
	 * @param stream The part's content
	 * @param length The part's length in bytes. -1 if unknown, as it always is for a part that's still arriving.
	 */
	public record Part( String name, String filename, Map<String, List<String>> headers, InputStream stream, long length ) {

		public Part {
			Objects.requireNonNull( headers );
			Objects.requireNonNull( stream );

			final Map<String, List<String>> caseInsensitiveHeaders = NGMessage.createEmptyHeadersMap();
			caseInsensitiveHeaders.putAll( headers );
			headers = Collections.unmodifiableMap( caseInsensitiveHeaders );
		}

		/**
		 * @return The part's content type. null if it has none.
		 */
		public String contentType() {
			final List<String> values = headers.get( "content-type" );
			return values == null || values.isEmpty() ? null : values.get( 0 );
		}

		/**
		 * @return true if the part is an uploaded file (even an empty one), false if it's a plain field
		 */
		public boolean isFile() {
			return filename != null;
		}
	}

	private final NGMultipartReader _reader;

	/**
	 * The request's parts, once collected. null until then.
	 */
	private List<Part> _parts;

	/**
	 * true once the parts have been handed over as they arrive, by incomingParts()
	 */
	private boolean _incoming;

	/**
	 * The request's uploaded files by name of field, once collected. null until then.
	 */
	private Map<String, List<Part>> _uploadedFiles;

	/**
	 * @param reader Reads the request's parts, the first time they're asked for
	 */
	public NGMultipartRequest( final String method, final String uri, final String queryString, final String httpVersion, final Map<String, List<String>> headers, final Map<String, List<String>> cookieValues, final NGMultipartReader reader, final String remoteAddress ) {
		super( method, uri, queryString, httpVersion, headers, cookieValues, InputStream.nullInputStream(), remoteAddress );
		Objects.requireNonNull( reader );
		_reader = reader;
	}

	/**
	 * @return Every part of the request, collected on first invocation
	 * @throws IllegalStateException If the parts have already been handed over by incomingParts()
	 */
	private synchronized List<Part> parts() {
		if( _incoming ) {
			throw new IllegalStateException( "This request's parts have already been read as they arrived, through incomingParts(). Its uploaded files can't be collected after that" );
		}

		if( _parts == null ) {
			_parts = _reader.readAll();
		}

		return _parts;
	}

	/**
	 * @return The request's parts as they arrive, for reading in one pass. A part's content is readable until the next part is asked for.
	 *
	 * The parts can be iterated once. formValues() then holds only the query string's form values, and uploadedFiles() is unavailable.
	 *
	 * @throws IllegalStateException If the parts have already been collected (by formValues() or uploadedFiles(), so invoke this first) or handed over
	 */
	public synchronized Iterable<Part> incomingParts() {
		if( _parts != null ) {
			throw new IllegalStateException( "This request's parts have already been collected (by formValues() or uploadedFiles()), so they can't be read as they arrive. Invoke incomingParts() before those" );
		}

		if( _incoming ) {
			throw new IllegalStateException( "This request's parts have already been handed over by incomingParts(). They can only be read once" );
		}

		_incoming = true;
		final Iterator<Part> iterator = _reader.iterate();

		return new Iterable<>() {
			private boolean _iterated;

			@Override
			public synchronized Iterator<Part> iterator() {
				if( _iterated ) {
					throw new IllegalStateException( "A request's incoming parts can only be iterated once" );
				}

				_iterated = true;
				return iterator;
			}
		};
	}

	/**
	 * @return The query string's form values, followed by the values of the form's fields (the parts that aren't files). Only the query string's if the parts have been handed over by incomingParts().
	 */
	@Override
	protected Map<String, List<String>> parseFormValues() {
		final Map<String, List<String>> formValues = super.parseFormValues();

		synchronized( this ) {
			if( _incoming ) {
				return formValues;
			}
		}

		for( final Part part : parts() ) {
			if( !part.isFile() ) {
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
	 * @return The uploaded files, by the name of the form field they were uploaded with. Their content stays readable until the request has been handled.
	 * @throws IllegalStateException If the parts have already been handed over by incomingParts()
	 */
	public synchronized Map<String, List<Part>> uploadedFiles() {
		if( _uploadedFiles == null ) {
			final Map<String, List<Part>> uploadedFiles = new LinkedHashMap<>();

			for( final Part part : parts() ) {
				if( part.isFile() ) {
					uploadedFiles.computeIfAbsent( part.name(), _unused -> new ArrayList<>() ).add( part );
				}
			}

			_uploadedFiles = uploadedFiles;
		}

		return _uploadedFiles;
	}

	/**
	 * @return The files uploaded with the named field. An empty list if there are none.
	 */
	public List<Part> uploadedFilesForKey( final String key ) {
		final List<Part> files = uploadedFiles().get( key );

		if( files == null ) {
			return Collections.emptyList();
		}

		return files;
	}

	/**
	 * @return The file uploaded with the named field. null if there's none.
	 * @throws IllegalStateException If more than one file was uploaded with the field
	 */
	public Part uploadedFileForKey( final String key ) {
		final List<Part> files = uploadedFilesForKey( key );

		if( files.isEmpty() ) {
			return null;
		}

		if( files.size() > 1 ) {
			throw new IllegalStateException( "The request contains %s files uploaded with the field '%s'. If you expected multiple files, use uploadedFilesForKey() instead of uploadedFileForKey()".formatted( files.size(), key ) );
		}

		return files.get( 0 );
	}

	/**
	 * A multipart request's content is its parts, read through formValues() and uploadedFiles(), or incomingParts()
	 *
	 * @throws UnsupportedOperationException Always
	 */
	@Override
	public ByteArrayOutputStream contentByteStream() {
		throw new UnsupportedOperationException( "A multipart request's content is its parts. Use formValues() and uploadedFiles(), or incomingParts()" );
	}
}
