package ng.appserver.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.properties.NGProperties;

/**
 * A multipart/form-data request. Its content is a list of parts: a form's fields, and the files uploaded with it.
 *
 * The parts can be read in one of two ways, whichever is asked for first:
 *
 * - Collected: formValues() and uploadedFiles() read every part before returning, keeping their content until the request has been handled: small parts in memory, larger ones in temporary files. Bounded by the request's Limits.
 * - Incoming: incomingParts() hands over the parts as they arrive, for reading in one pass. For files too large to collect, or to handle while they're still arriving.
 *   The form's fields then arrive as parts too, and formValues() holds only the query string's.
 */

public class NGMultipartRequest extends NGStandardRequest {

	private static final Logger logger = LoggerFactory.getLogger( NGMultipartRequest.class );

	/**
	 * Bounds on collecting a request's parts. Parts read as they arrive (incomingParts()) are the handler's to bound.
	 *
	 * @param maxParts The most parts a request may have
	 * @param maxContentLength The most bytes all the parts' content may add up to. -1 for no limit.
	 * @param maxMemoryPartLength The largest part kept in memory, in bytes. Larger parts are kept in temporary files.
	 * @param temporaryDirectory Where the temporary files are kept
	 */
	public record Limits( int maxParts, long maxContentLength, int maxMemoryPartLength, Path temporaryDirectory ) {

		public static final Limits DEFAULT = new Limits( 1000, 1024L * 1024 * 1024, 64 * 1024, Path.of( System.getProperty( "java.io.tmpdir" ) ) );

		/**
		 * @return The limits set by the properties ng.multipart.maxParts, ng.multipart.maxContentLength, ng.multipart.maxMemoryPartLength and ng.multipart.temporaryDirectory, DEFAULT's where they're not set
		 */
		public static Limits from( final NGProperties properties ) {
			final String maxParts = properties.get( "ng.multipart.maxParts" );
			final String maxContentLength = properties.get( "ng.multipart.maxContentLength" );
			final String maxMemoryPartLength = properties.get( "ng.multipart.maxMemoryPartLength" );
			final String temporaryDirectory = properties.get( "ng.multipart.temporaryDirectory" );

			return new Limits(
					maxParts == null ? DEFAULT.maxParts() : Integer.parseInt( maxParts ),
					maxContentLength == null ? DEFAULT.maxContentLength() : Long.parseLong( maxContentLength ),
					maxMemoryPartLength == null ? DEFAULT.maxMemoryPartLength() : Integer.parseInt( maxMemoryPartLength ),
					temporaryDirectory == null ? DEFAULT.temporaryDirectory() : Path.of( temporaryDirectory ) );
		}
	}

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

	/**
	 * The request's body, split into parts by the parser
	 */
	private final InputStream _body;
	private final String _boundary;
	private final NGMultipartParser _parser;
	private final Limits _limits;

	/**
	 * Temporary files holding collected parts, and the streams reading them, deleted and closed by _close()
	 */
	private final List<Path> _temporaryFiles = new ArrayList<>();
	private final List<InputStream> _temporaryFileStreams = new ArrayList<>();

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
	 * @param body The request's body. Readable only while the adaptor is handling the request.
	 * @param boundary The boundary between the parts, from the request's content type
	 * @param parser Splits the body into its parts, the first time they're asked for
	 */
	public NGMultipartRequest( final String method, final String uri, final String queryString, final String httpVersion, final Map<String, List<String>> headers, final InputStream body, final String boundary, final NGMultipartParser parser, final Limits limits, final String remoteAddress ) {
		super( method, uri, queryString, httpVersion, headers, InputStream.nullInputStream(), remoteAddress );
		Objects.requireNonNull( body );
		Objects.requireNonNull( boundary );
		Objects.requireNonNull( parser );
		Objects.requireNonNull( limits );
		_body = body;
		_boundary = boundary;
		_parser = parser;
		_limits = limits;
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
			_parts = collect();
		}

		return _parts;
	}

	/**
	 * @return Every part of the request, read in full and kept: in memory up to the limits' maxMemoryPartLength, in a temporary file beyond it
	 * @throws IllegalArgumentException If the parts exceed the limits
	 */
	private List<Part> collect() {
		final List<Part> parts = new ArrayList<>();
		final Iterator<Part> incoming = _parser.parse( _body, _boundary );
		long contentLength = 0;

		while( incoming.hasNext() ) {
			if( parts.size() == _limits.maxParts() ) {
				throw new IllegalArgumentException( "Multipart request has more than %s parts".formatted( _limits.maxParts() ) );
			}

			final Part part = keep( incoming.next(), contentLength );
			contentLength += part.length();
			parts.add( part );
		}

		return parts;
	}

	/**
	 * @param contentLength The length of the parts kept before this one
	 * @return A copy of the given part whose content is kept, readable until the request is closed
	 */
	private Part keep( final Part part, final long contentLength ) {
		final ByteArrayOutputStream memory = new ByteArrayOutputStream();
		OutputStream out = memory;
		Path file = null;
		long length = 0;

		try( final InputStream in = part.stream()) {
			final byte[] buffer = new byte[8192];
			int read;

			while( (read = in.read( buffer )) != -1 ) {
				length += read;

				if( _limits.maxContentLength() != -1 && contentLength + length > _limits.maxContentLength() ) {
					throw new IllegalArgumentException( "Multipart request content is longer than %s bytes".formatted( _limits.maxContentLength() ) );
				}

				// The part outgrew memory, so it moves to a temporary file
				if( file == null && length > _limits.maxMemoryPartLength() ) {
					file = temporaryFile();
					out = Files.newOutputStream( file );
					memory.writeTo( out );
				}

				out.write( buffer, 0, read );
			}
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
		finally {
			if( file != null ) {
				try {
					out.close();
				}
				catch( IOException e ) {
					logger.warn( "Couldn't close temporary file {}", file, e );
				}
			}
		}

		final InputStream content = file == null ? new ByteArrayInputStream( memory.toByteArray() ) : temporaryFileStream( file );
		return new Part( part.name(), part.filename(), part.headers(), content, length );
	}

	private synchronized Path temporaryFile() throws IOException {
		final Path file = Files.createTempFile( _limits.temporaryDirectory(), "ng-multipart-", ".tmp" );
		_temporaryFiles.add( file );
		return file;
	}

	private synchronized InputStream temporaryFileStream( final Path file ) {
		try {
			final InputStream stream = Files.newInputStream( file );
			_temporaryFileStreams.add( stream );
			return stream;
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Deletes the temporary files holding collected parts
	 */
	@Override
	public synchronized void _close() {
		for( final InputStream stream : _temporaryFileStreams ) {
			try {
				stream.close();
			}
			catch( IOException e ) {
				logger.warn( "Couldn't close a stream of a temporary file", e );
			}
		}

		for( final Path file : _temporaryFiles ) {
			try {
				Files.deleteIfExists( file );
			}
			catch( IOException e ) {
				logger.warn( "Couldn't delete temporary file {}", file, e );
			}
		}

		_temporaryFileStreams.clear();
		_temporaryFiles.clear();
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
		final Iterator<Part> iterator = _parser.parse( _body, _boundary );

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
