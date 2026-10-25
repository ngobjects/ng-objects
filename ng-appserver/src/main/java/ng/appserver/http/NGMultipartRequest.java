package ng.appserver.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
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
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGContext;

/**
 * A multipart/form-data request. Its body is a list of parts: a form's fields, and the files uploaded with it.
 *
 * Besides the ways of reading a body every request has (see NGRequest), its parts can be read:
 *
 * - Collected: formValues() and uploadedFiles() read every part and keep its content until the request has been handled: small parts in memory, larger ones in temporary files. Bounded by the request's limits (ng.request.* properties).
 * - Incoming: incomingParts() hands over the parts as they arrive, for files too large to collect, or to handle while they're still arriving. Unless the body was already kept, it then isn't, and form values hold only the query string's.
 */

public class NGMultipartRequest implements NGRequest {

	private static final Logger logger = LoggerFactory.getLogger( NGMultipartRequest.class );

	/**
	 * One part of a multipart request: a form field, or an uploaded file
	 */
	public static final class Part {

		private final String _name;
		private final String _filename;
		private final Map<String, List<String>> _headers;
		private final Supplier<InputStream> _content;
		private final Long _length;

		/**
		 * @param name The name of the form field the part belongs to
		 * @param filename The uploaded file's name, if the part is a file. Empty when a form's file field was submitted without a file. null if the part is a plain field.
		 * @param headers The part's headers
		 * @param content Supplies the part's content: a new stream on each invocation if the content is kept, the same one-shot stream if it's arriving
		 * @param length The part's length in bytes. null if unknown, as it is for a part that's still arriving.
		 */
		public Part( final String name, final String filename, final Map<String, List<String>> headers, final Supplier<InputStream> content, final Long length ) {
			Objects.requireNonNull( headers );
			Objects.requireNonNull( content );

			final Map<String, List<String>> caseInsensitiveHeaders = NGHeaderMaps.create();
			caseInsensitiveHeaders.putAll( headers );

			_name = name;
			_filename = filename;
			_headers = Collections.unmodifiableMap( caseInsensitiveHeaders );
			_content = content;
			_length = length;
		}

		/**
		 * @return The name of the form field the part belongs to
		 */
		public String name() {
			return _name;
		}

		/**
		 * @return The uploaded file's name, if the part is a file. Empty when a form's file field was submitted without a file. null if the part is a plain field.
		 */
		public String filename() {
			return _filename;
		}

		/**
		 * @return The part's headers. Names are case-insensitive.
		 */
		public Map<String, List<String>> headers() {
			return _headers;
		}

		/**
		 * @return The part's content. For a kept part (collected, or uploaded files) a new stream on each invocation; for a part read as it arrives (incomingParts()) the same stream, readable once, until the next part is asked for.
		 */
		public InputStream stream() {
			return _content.get();
		}

		/**
		 * @return The part's length in bytes. null if unknown, as it is for a part that's still arriving.
		 */
		public Long length() {
			return _length;
		}

		/**
		 * @return The part's content type. null if it has none.
		 */
		public String contentType() {
			final List<String> values = _headers.get( "content-type" );
			return values == null || values.isEmpty() ? null : values.get( 0 );
		}

		/**
		 * @return true if the part is an uploaded file (even an empty one), false if it's a plain field
		 */
		public boolean isFile() {
			return _filename != null;
		}

		@Override
		public String toString() {
			return "Part [name=" + _name + ", filename=" + _filename + ", length=" + _length + "]";
		}
	}

	/**
	 * A part whose content has been kept, which can be read any number of times
	 *
	 * @param content The part's content if it's kept in memory. null if it's kept in a temporary file.
	 * @param file The temporary file the part's content is kept in. null if it's kept in memory.
	 */
	private record KeptPart( String name, String filename, Map<String, List<String>> headers, byte[] content, Path file, long length ) {

		/**
		 * @return The charset named by the part's content type, UTF-8 if it names none
		 */
		Charset charset() {
			final List<String> contentTypes = headers.get( "content-type" );
			return contentTypes == null || contentTypes.isEmpty() ? StandardCharsets.UTF_8 : NGRequestHead.charset( contentTypes.get( 0 ) );
		}
	}

	/**
	 * The request's line, headers and origin, and what's parsed from them: everything about the request apart from its body
	 */
	private final NGRequestHead _head;

	/**
	 * The request's body as it arrived
	 */
	private final NGRequestBody _body;

	/**
	 * The boundary between the body's parts, from the request's content type
	 */
	private final String _boundary;

	/**
	 * Splits the body into its parts
	 */
	private final NGMultipartParser _parser;

	private final NGRequestLimits _limits;

	/**
	 * The request's parts, once collected. null until then.
	 */
	private List<KeptPart> _keptParts;

	/**
	 * The request's form values, from the query string and the form's fields. null until something asks for them.
	 */
	private Map<String, List<String>> _formValues;

	/**
	 * The request's uploaded files by name of field, once collected. null until then.
	 */
	private Map<String, List<Part>> _uploadedFiles;

	/**
	 * Temporary files holding collected parts, and the streams reading them, closed and deleted by _close()
	 */
	private final List<Path> _temporaryFiles = new ArrayList<>();
	private final List<InputStream> _temporaryFileStreams = new ArrayList<>();

	/**
	 * @param body The request's body. Readable only while the adaptor is handling the request.
	 * @param boundary The boundary between the parts, from the request's content type
	 * @param parser Splits the body into its parts
	 */
	NGMultipartRequest( final String method, final String uri, final String queryString, final Map<String, List<String>> headers, final InputStream body, final String boundary, final NGMultipartParser parser, final NGRequestLimits limits, final String remoteAddress ) {
		Objects.requireNonNull( boundary );
		Objects.requireNonNull( parser );
		Objects.requireNonNull( limits );
		_head = new NGRequestHead( method, uri, queryString, headers, remoteAddress );
		_body = new NGRequestBody( body );
		_boundary = boundary;
		_parser = parser;
		_limits = limits;
	}

	@Override
	public String method() {
		return _head.method();
	}

	@Override
	public String uri() {
		return _head.uri();
	}

	@Override
	public void setURI( final String uri ) {
		_head.setURI( uri );
	}

	@Override
	public Map<String, List<String>> headers() {
		return _head.headers();
	}

	@Override
	public void setHeaders( final Map<String, List<String>> headers ) {
		_head.setHeaders( headers );
	}

	@Override
	public String remoteAddress() {
		return _head.remoteAddress();
	}

	@Override
	public Map<String, List<String>> cookieValues() {
		return _head.cookieValues();
	}

	@Override
	public NGContext context() {
		return _head.context( this );
	}

	@Override
	public void _setContext( final NGContext context ) {
		_head.setContext( context );
	}

	/**
	 * @return The body as it arrived, boundaries and all, read in full and kept. The parts can still be read afterwards, from what was kept.
	 * @throws IllegalStateException If the body was already read in a way that didn't keep it, which includes collecting its parts
	 */
	@Override
	public byte[] contentBytes() {
		return _body.bytes( null ).clone();
	}

	/**
	 * @return The body as it arrived, as a stream
	 * @throws IllegalStateException If the body was already read in a way that didn't keep it, which includes collecting its parts
	 */
	@Override
	public InputStream contentStream() {
		return _body.stream( "contentStream()" );
	}

	/**
	 * @return The query string's form values, followed by the values of the form's fields (the parts that aren't files). Only the query string's if the body was read as a stream. Parsed on first invocation.
	 * @throws IllegalArgumentException If the query string has more fields, or the body more parts or bytes, than the request's limits allow
	 */
	@Override
	public synchronized Map<String, List<String>> formValues() {
		if( _formValues == null ) {
			_formValues = parseFormValues();
		}

		return _formValues;
	}

	private Map<String, List<String>> parseFormValues() {
		final Map<String, List<String>> formValues = _head.parseQueryValues( _limits.maxFormFields() );

		_body.requireNotFailed( "collected as parts" );

		if( _keptParts == null && _body.isStreamed() ) {
			return formValues;
		}

		for( final KeptPart part : keptParts() ) {
			if( part.filename() == null ) {
				try( final InputStream stream = open( part )) {
					formValues.computeIfAbsent( part.name(), _unused -> new ArrayList<>() ).add( new String( stream.readAllBytes(), part.charset() ) );
				}
				catch( IOException e ) {
					throw new UncheckedIOException( e );
				}
			}
		}

		return formValues;
	}

	/**
	 * @return The uploaded files, by the name of the form field they were uploaded with. Their content can be read any number of times until the request has been handled.
	 * @throws IllegalStateException If the body was already read in a way that didn't keep it
	 * @throws IllegalArgumentException If the body has more parts or bytes than the request's limits allow
	 */
	public synchronized Map<String, List<Part>> uploadedFiles() {
		if( _uploadedFiles == null ) {
			final Map<String, List<Part>> uploadedFiles = new LinkedHashMap<>();

			for( final KeptPart part : keptParts() ) {
				if( part.filename() != null ) {
					uploadedFiles.computeIfAbsent( part.name(), _unused -> new ArrayList<>() ).add( part( part ) );
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
	 * @return The first file uploaded with the named field. null if there's none. Several files uploaded with one field isn't an error: uploadedFilesForKey() has every file.
	 */
	public Part uploadedFileForKey( final String key ) {
		final List<Part> files = uploadedFilesForKey( key );

		if( files.isEmpty() ) {
			return null;
		}

		return files.get( 0 );
	}

	/**
	 * @return The request's parts, for reading in one pass. Served from what's kept if the body or its parts already are; otherwise read as they arrive, a part's content readable until the next part is asked for, after which the body isn't kept and form values hold only the query string's.
	 * @throws IllegalStateException If the body was already read in a way that didn't keep it
	 */
	public synchronized Iterable<Part> incomingParts() {
		final Iterator<Part> iterator;

		if( _keptParts != null ) {
			iterator = _keptParts.stream().map( this::part ).iterator();
		}
		else {
			iterator = _parser.parse( _body.stream( "incomingParts()" ), _boundary );
		}

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
	 * @return Every part of the request, collected and kept on first invocation: read from the kept body if it's kept, otherwise from the body as it arrives
	 * @throws IllegalStateException If the body was already read in a way that didn't keep it
	 * @throws IllegalArgumentException If the body has more parts or bytes than the request's limits allow
	 */
	private synchronized List<KeptPart> keptParts() {
		if( _keptParts == null ) {
			_body.requireReadable( "collected as parts" );
			final InputStream source = _body.stream( "formValues() or uploadedFiles(), collecting its parts" );

			try {
				_keptParts = collect( _parser.parse( source, _boundary ) );
			}
			catch( RuntimeException e ) {
				_body.fail( "collecting its parts failed (%s)".formatted( e.getMessage() ) );
				throw e;
			}
		}

		return _keptParts;
	}

	/**
	 * @return The given parts, read in full and kept: in memory up to the limits' maxMemoryPartLength, in a temporary file beyond it
	 * @throws IllegalArgumentException If the parts exceed the limits
	 */
	private List<KeptPart> collect( final Iterator<Part> incoming ) {
		final List<KeptPart> parts = new ArrayList<>();
		long contentLength = 0;

		while( incoming.hasNext() ) {
			if( parts.size() == _limits.maxParts() ) {
				throw new IllegalArgumentException( "Multipart request has more than %s parts".formatted( _limits.maxParts() ) );
			}

			final KeptPart part = keep( incoming.next(), contentLength );
			contentLength += part.length();
			parts.add( part );
		}

		return parts;
	}

	/**
	 * @param contentLength The length of the parts kept before this one
	 * @return The given part with its content kept, readable until the request is closed
	 */
	private KeptPart keep( final Part part, final long contentLength ) {
		final ByteArrayOutputStream memory = new ByteArrayOutputStream();
		OutputStream out = memory;
		Path file = null;
		long length = 0;

		try( final InputStream in = part.stream()) {
			final byte[] buffer = new byte[8192];
			int read;

			while( (read = in.read( buffer )) != -1 ) {
				length += read;

				if( _limits.maxMultipartContentLength() != null && contentLength + length > _limits.maxMultipartContentLength() ) {
					throw new IllegalArgumentException( "Multipart request content is longer than %s bytes".formatted( _limits.maxMultipartContentLength() ) );
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

		return new KeptPart( part.name(), part.filename(), part.headers(), file == null ? memory.toByteArray() : null, file, length );
	}

	/**
	 * @return The given kept part, opening a new stream over its content on each read
	 */
	private Part part( final KeptPart part ) {
		return new Part( part.name(), part.filename(), part.headers(), () -> open( part ), part.length() );
	}

	/**
	 * @return A new stream over the given kept part's content
	 */
	private synchronized InputStream open( final KeptPart part ) {
		if( part.content() != null ) {
			return new ByteArrayInputStream( part.content() );
		}

		try {
			final InputStream stream = Files.newInputStream( part.file() );
			_temporaryFileStreams.add( stream );
			return stream;
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private synchronized Path temporaryFile() throws IOException {
		final Path file = Files.createTempFile( _limits.temporaryDirectory(), "ng-multipart-", ".tmp" );
		_temporaryFiles.add( file );
		return file;
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

	@Override
	public String toString() {
		return "NGMultipartRequest [" + _head.describe() + "]";
	}
}
