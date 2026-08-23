package ng.appserver;

import ng.appserver.http.NGRequest;
import ng.appserver.http.NGResponse;

/**
 * FIXME: This should really be an interface // Hugi 2025-04-19
 */

public abstract class NGRequestHandler {

	public abstract NGResponse handleRequest( NGRequest request );
}