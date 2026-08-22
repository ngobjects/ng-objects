package ng.appserver;

import java.util.List;
import java.util.Map;

import ng.appserver.NGStandardRequest.UploadedFile;
import ng.appserver.privates.NGParsedURI;

/**
 * Represents a request entering the system
 */

public interface NGRequest extends NGMessage {

	/**
	 * Name of the cookie that stores our session ID on the client
	 */
	public static final String SESSION_ID_COOKIE_NAME = "ngsid";

	public Map<String, List<String>> formValues();

	public List<String> formValuesForKey( final String key );

	public String formValueForKey( final String key );

	public String uri();

	public void setURI( final String uri );

	public String method();

	public void setMethod( final String method );

	/**
	 * @return The network address of the client that sent this request, as reported by the adaptor, or null when the adaptor didn't provide one.
	 *
	 * FIXME: I'm not happy with these two. They arrived as a default method + an adaptor-populated
	 * setter only to give the dev endpoints an address to loopback-check (/ng/dev/eval), and both
	 * of those shapes are smells: a default returning null is a "not really part of the contract
	 * yet" marker, and having the *adaptor* set this is the same complaint as the cookie/form-value
	 * setters below — the request should model the client address itself, not receive it. Revisit
	 * when NGRequest's model settles (it's still in flux). // Hugi 2026-08-10
	 */
	public default String remoteAddress() {
		return null;
	}

	public default void _setRemoteAddress( final String remoteAddress ) {}

	public String _sessionID();

	public NGSession session();

	public NGSession existingSession();

	public boolean hasSession();

	public Map<String, List<String>> cookieValues();

	public List<String> cookieValuesForKey( final String key );

	public String cookieValueForKey( final String key );

	public NGContext context();

	public void setContext( NGContext context );

	public NGParsedURI parsedURI();

	public Map<String, UploadedFile> _uploadedFiles();
}