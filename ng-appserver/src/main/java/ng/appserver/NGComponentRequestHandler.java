package ng.appserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGComponentRequestHandler extends NGRequestHandler {

	private static Logger logger = LoggerFactory.getLogger( NGComponentRequestHandler.class );

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		NGContext c = request.context();
		logger.debug( "uri: " + request.uri() );
		logger.debug( "context: {} : {}", c.contextID(), c.page() );
		logger.debug( "originating context: {} : {}", c._originalContext.contextID(), c._originalContext.page() );

		request.context()._originalContext.page().takeValuesFromRequest( request, request.context() );
		final NGActionResults actionResults = request.context()._originalContext.page().invokeAction( request, request.context() );

		if( actionResults == null ) {
			logger.debug( "Action method returned null, invoking generateResponse on the original page" );
			return request.context()._originalContext.page().generateResponse();
		}

		return actionResults.generateResponse();
	}
}