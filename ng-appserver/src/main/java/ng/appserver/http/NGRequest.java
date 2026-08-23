package ng.appserver.http;

import java.util.List;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.NGSession;
import ng.appserver.http.NGStandardRequest.UploadedFile;
import ng.appserver.privates.NGParsedURI;

/**
 * Represents a request entering the system
 */

public interface NGRequest extends NGMessage {

	public String method();

	public String uri();

	/**
	 * FIXME: Delete. NGRequest really wants to be an immutable interface, awaiting changes in the request/response data model as a whole // Hugi 2026-08-23
	 */
	@Deprecated
	public void setURI( final String uri );

	public Map<String, List<String>> formValues();

	public List<String> formValuesForKey( final String key );

	public String formValueForKey( final String key );

	public Map<String, List<String>> cookieValues();

	public List<String> cookieValuesForKey( final String key );

	public String cookieValueForKey( final String key );

	public NGContext context();

	/**
	 * FIXME: Delete. NGRequest really wants to be an immutable interface, awaiting changes in the request/response data model as a whole // Hugi 2026-08-23
	 */
	@Deprecated
	public void _setContext( NGContext context );

	public String _sessionID();

	public NGSession session();

	public NGSession existingSession();

	public boolean hasSession();

	/**
	 * @return The network address of the client that sent this request, as reported by the adaptor, or null when the adaptor didn't provide one.
	 */
	public String remoteAddress();

	/**
	 * @return The request's URI, parsed
	 *
	 * @deprecated URL parsing is probably better handled by consumers
	 */
	@Deprecated
	public NGParsedURI parsedURI();

	public Map<String, UploadedFile> _uploadedFiles();
}