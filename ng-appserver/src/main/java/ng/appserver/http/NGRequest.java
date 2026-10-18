package ng.appserver.http;

import java.util.List;
import java.util.Map;

import ng.appserver.NGContext;
import ng.appserver.http.NGStandardRequest.UploadedFile;

/**
 * Represents a request entering the system
 */

public interface NGRequest extends NGMessage {

	public String method();

	public String uri();

	public Map<String, List<String>> formValues();

	public List<String> formValuesForKey( final String key );

	public String formValueForKey( final String key );

	public Map<String, List<String>> cookieValues();

	public List<String> cookieValuesForKey( final String key );

	public String cookieValueForKey( final String key );

	public NGContext context();

	/**
	 * @return The sessionID submitted by the client, if any. null if no sessionID was present in the request.
	 */
	public String _sessionID();

	/**
	 * @return The network address of the client that sent this request, as reported by the adaptor, or null when the adaptor didn't provide one.
	 */
	public String remoteAddress();

	/**
	 * FIXME: Delete. NGRequest really wants to be an immutable interface, awaiting changes in the request/response data model as a whole // Hugi 2026-08-23
	 */
	@Deprecated
	public void setURI( final String uri );

	/**
	 * FIXME: Delete. NGRequest really wants to be an immutable interface, awaiting changes in the request/response data model as a whole // Hugi 2026-08-23
	 */
	@Deprecated
	public void _setContext( NGContext context );

	/**
	 * FIXME: This will be deleted once we have a proper representation of multipart requests // Hugi 2026-08-23
	 */
	@Deprecated
	public Map<String, UploadedFile> _uploadedFiles();
}