package ng.appserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles requests for the "stateful" part of the framework.
 *
 * The basics:
 *
 * - URLs for "stateful requests" contain only numbers, separated by a period (eg. 3.4.12.5)
 * - The first number is the "contextID". In simplistic terms, this identifies an instance of a page/NGComponent (e.g. '3')
 * - The remaining numbers comprise the "senderID". It identifies a method invoked on the page/NGComponent instance, usually by clicking a link or submitting a form (e.g. '4.12.5')
 * - The stateful pages are stored in the user's session, basically a Map that maps contextIDs to page instances.
 */

public class NGComponentRequestHandler extends NGRequestHandler {

	private static Logger logger = LoggerFactory.getLogger( NGComponentRequestHandler.class );

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		// At this point, this point, the request's context is a freshly created one
		final NGContext context = request.context();

		if( context == null ) {
			throw new IllegalStateException( "The request's context is null. This should never happen" );
		}

		// We need to use the session to gain access to the page cache
		final NGSession session = context.session();

		if( session == null ) {
			throw new IllegalStateException( "The context's session is null. That should never happen" );
		}

		// Now let's try to restore the page from the cache
		final NGComponent originalPage = session.restorePageFromCache( context._originatingContextID() );

		// FIXME: This will be hit once the page has been pushed out of the session's page cache.
		// This will be common enough that it needs it's own separate handling.
		if( originalPage == null ) {
			throw new IllegalStateException( "No page found in the page cache for contextID %s. The page has probably been pushed out of the session's page cache".formatted( context._originatingContextID() ) );
		}

		logger.info( "Page restored from cache is: " + originalPage.getClass() );

		// Push the page in the context
		context.setPage( originalPage );
		context.setCurrentComponent( originalPage );
		context.page().awakeInContext( request.context() );

		logger.debug( "About to perform takeValuesfromRequest in context {} on page {} ", context._originatingContextID(), originalPage );
		// FIXME: We can probably save a few cycles by only performing takeValuesFromRequest if there are, you know, actual values in the request to take // Hugi 2023-01-07
		originalPage.takeValuesFromRequest( request, context );

		logger.debug( "About to perform invokeAction on element {} in context {} on page {} ", context.senderID(), context._originatingContextID(), originalPage );

		// We now invoke the action on the original page instance
		final NGActionResults actionInvocationResults = originalPage.invokeAction( request, context );

		logger.debug( "Action invocation returned {}", actionInvocationResults );

		// The response we will eventually return
		NGResponse response;

		// The response returned by an action can be
		// - null : Meaning we're working within a page/staying on the same page instance
		// - An instance of NGComponent : In which case that becomes the new page of this context
		// - Everything else that implements NGActionResults : which we just allow to do it's own thing (by invoking generateResponse() on it)

		if( actionInvocationResults == null ) {
			// If an action returns null, we're staying on the same page
			logger.debug( "Action method returned null, invoking generateResponse on the original page" );
			response = originalPage.generateResponse();
		}
		else if( actionInvocationResults instanceof NGComponent newPage ) {
			// If an action method returns an NGComponent, that's our new page in this context. We set it, and return it
			context.setPage( newPage );
			context.setCurrentComponent( newPage );
			newPage.awakeInContext( context );
			response = newPage.generateResponse();
		}
		else {
			// If this is not a WOComponent, we don't need to take any special action and just invoke generateResponse() on the action's results
			response = actionInvocationResults.generateResponse();
		}

		if( response == null ) {
			throw new IllegalStateException( "Response is null. This should never happen" );
		}

		return response;
	}
}