package ng.appserver.http;

import java.util.Iterator;
import java.util.List;

import ng.appserver.http.NGMultipartRequest.Part;

/**
 * Reads a multipart request's parts. Implemented by the adaptor, for NGMultipartRequest, which invokes one of these methods, once, while the adaptor is handling the request.
 */

public interface NGMultipartReader {

	/**
	 * @return Every part of the request, read in full. Their content stays readable until the request has been handled.
	 */
	public List<Part> readAll();

	/**
	 * @return The request's parts, read as they arrive. A part's content is readable until the next part is asked for, and its length is unknown (-1).
	 */
	public Iterator<Part> iterate();
}
