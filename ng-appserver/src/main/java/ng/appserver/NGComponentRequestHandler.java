package ng.appserver;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGComponentRequestHandler extends NGRequestHandler {

	private static Logger logger = LoggerFactory.getLogger( NGComponentRequestHandler.class );

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		// at this point, this point, the request's context is a freshly created one
		logger.debug( "request.context: " + request.context() );
		logger.debug( "request.originatingContext: " + request.context().originatingContext() );
		logger.debug( "pageCache: " + _pageCache );

		final String pageKey = request.context().originatingContext().contextID() + "." + request.context().senderID();

		logger.debug( "Our pageKey is: " + pageKey );

		// Now let's try to restore the page from the cache
		NGComponent page = restorePageFromCache( pageKey );

		if( page == null ) {
			logger.debug( "Page '{}' not found in cache, generating", pageKey );
			// If no page was found, we're going to have to generate it
			page = request.context().originatingContext().page();
			savePage( pageKey, page );
		}
		else {
			logger.debug( "Page '{}' found in cache", pageKey );
		}

		//		request.context().setPage( page );
		//		request.context().setCurrentComponent( page );
		//		request.context().page().awakeInContext( request.context() );

		page.takeValuesFromRequest( request, request.context() );

		logger.debug( "About to perform invokeAction on element {} in context {} on page {} ", request.context().senderID(), request.context().originatingContext().contextID(), page );

		final NGActionResults actionResults = page.invokeAction( request, request.context() );

		if( actionResults == null ) {
			logger.debug( "Action method returned null, invoking generateResponse on the original page" );
			return page.generateResponse();
		}

		return actionResults.generateResponse();
	}

	public static String pageCacheKey( String contextID, String senderID ) {
		String key = contextID;

		if( senderID != null ) {
			key = key + "." + senderID;
		}

		return key;
	}

	/**
	 * FIXME: OK, this is horrible, but we're going to start out with out pageCache here. This belongs in the session, really.
	 *
	 * The page cache is going to have to keep track of
	 *
	 *  1. The originating context ID
	 *  2. The elementID the page originates from (for example, the click of a link)
	 *
	 *  So, let's just for now store the page as an accumulation of the entire string after the request handler key
	 */
	public static Map<String, NGComponent> _pageCache = new HashMap<>();

	public static void savePage( String key, NGComponent component ) {
		logger.debug( "Saving page ing cache with key: " + key );
		_pageCache.put( key, component );
	}

	public static NGComponent restorePageFromCache( String key ) {
		logger.debug( "Restoring page from cache with key: " + key );
		return _pageCache.get( key );
	}
}