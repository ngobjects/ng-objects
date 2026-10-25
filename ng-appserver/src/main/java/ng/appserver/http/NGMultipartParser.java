package ng.appserver.http;

import java.io.InputStream;
import java.util.Iterator;

import ng.appserver.http.NGMultipartRequest.Part;

/**
 * Splits a multipart body into its parts. Provided by the adaptor.
 */

@FunctionalInterface
public interface NGMultipartParser {

	/**
	 * @param body The request's body
	 * @param boundary The boundary between the parts, from the request's content type
	 * @return The body's parts, read from the body as they're asked for. A part's content is readable until the next part is asked for, and its length is unknown (-1).
	 */
	public Iterator<Part> parse( InputStream body, String boundary );
}
