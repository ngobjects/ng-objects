package ng.appserver.http;

import java.nio.file.Path;
import java.util.Objects;

import ng.appserver.properties.NGProperties;

/**
 * Bounds on reading a request's form values and collecting its parts, set by properties. Reading a body as bytes or as a stream is the handler's to bound.
 *
 * @param maxFormFields The most fields read from the query string, and from a form-encoded body (ng.request.maxFormFields)
 * @param maxFormContentLength The longest form-encoded body read for form values, in bytes (ng.request.maxFormContentLength)
 * @param maxParts The most parts collected from a multipart body (ng.request.maxParts)
 * @param maxMultipartContentLength The most bytes collected parts may add up to. null for no limit (ng.request.maxMultipartContentLength, "none" for no limit).
 * @param maxMemoryPartLength The largest collected part kept in memory, in bytes. Larger parts are kept in temporary files (ng.request.maxMemoryPartLength).
 * @param temporaryDirectory Where those temporary files are kept (ng.request.temporaryDirectory)
 */
record NGRequestLimits( int maxFormFields, int maxFormContentLength, int maxParts, Long maxMultipartContentLength, int maxMemoryPartLength, Path temporaryDirectory ) {

	static final NGRequestLimits DEFAULT = new NGRequestLimits( 1000, 200_000, 1000, 1024L * 1024 * 1024, 64 * 1024, Path.of( System.getProperty( "java.io.tmpdir" ) ) );

	NGRequestLimits {
		Objects.requireNonNull( temporaryDirectory );
	}

	/**
	 * @return The limits set by the given properties, DEFAULT's where they're not set
	 */
	static NGRequestLimits from( final NGProperties properties ) {
		final String maxFormFields = properties.get( "ng.request.maxFormFields" );
		final String maxFormContentLength = properties.get( "ng.request.maxFormContentLength" );
		final String maxParts = properties.get( "ng.request.maxParts" );
		final String maxMultipartContentLength = properties.get( "ng.request.maxMultipartContentLength" );
		final String maxMemoryPartLength = properties.get( "ng.request.maxMemoryPartLength" );
		final String temporaryDirectory = properties.get( "ng.request.temporaryDirectory" );

		return new NGRequestLimits(
				maxFormFields == null ? DEFAULT.maxFormFields() : Integer.parseInt( maxFormFields ),
				maxFormContentLength == null ? DEFAULT.maxFormContentLength() : Integer.parseInt( maxFormContentLength ),
				maxParts == null ? DEFAULT.maxParts() : Integer.parseInt( maxParts ),
				maxMultipartContentLength == null ? DEFAULT.maxMultipartContentLength() : "none".equalsIgnoreCase( maxMultipartContentLength ) ? null : Long.valueOf( maxMultipartContentLength ),
				maxMemoryPartLength == null ? DEFAULT.maxMemoryPartLength() : Integer.parseInt( maxMemoryPartLength ),
				temporaryDirectory == null ? DEFAULT.temporaryDirectory() : Path.of( temporaryDirectory ) );
	}
}
