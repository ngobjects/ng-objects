package ng.appserver.resources;

import java.io.InputStream;

/**
 * Represents a cached in-memory resource.
 */

public record NGDynamicResource(
		String filename,
		InputStream inputStream,
		String mimeType,
		Long length ) {}