package ng.appserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		if( originalPage == null ) {
			throw new IllegalStateException( "No page found in cache" );
		}

		logger.info( "Page restored from cache is: " + originalPage.getClass() );

		// At this point, we must know what page we're working with.
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

		// If action results are null, we're returning the same page
		if( actionInvocationResults == null ) {
			logger.debug( "Action method returned null, invoking generateResponse on the original page" );
			response = originalPage.generateResponse();
		}
		else if( actionInvocationResults instanceof NGComponent newPage ) {
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
			throw new IllegalStateException( "Response is null, there's something we haven't handled yet" );
		}

		return response;
	}
}