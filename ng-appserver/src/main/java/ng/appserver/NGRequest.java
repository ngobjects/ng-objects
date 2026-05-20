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

	public void _setFormValues( final Map<String, List<String>> formValues );

	public String uri();

	public void setURI( final String uri );

	public String method();

	public void setMethod( final String method );

	public String _sessionID();

	public NGSession session();

	public NGSession existingSession();

	public boolean hasSession();

	public Map<String, List<String>> cookieValues();

	public void _setCookieValues( Map<String, List<String>> cookieValues );

	public List<String> cookieValuesForKey( final String key );

	public String cookieValueForKey( final String key );

	public NGContext context();

	public void setContext( NGContext context );

	public NGParsedURI parsedURI();

	public Map<String, UploadedFile> _uploadedFiles();
}