package ng.appserver.http;

import java.io.InputStream;

/**
 * A file uploaded with a multipart request
 *
 * @param filename The file's name, as given by the client. Empty when a form's file field was submitted without a file.
 * @param contentType The file's content type, as given by the client. null if it gave none.
 * @param stream The file's content
 * @param length The file's length in bytes
 */
public record NGUploadedFile( String filename, String contentType, InputStream stream, long length ) {}
